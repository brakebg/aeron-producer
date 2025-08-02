package com.example.aeronproducer.service;

import com.example.aeronproducer.metrics.LatencyMetrics;
import com.example.aeronproducer.model.FinnhubTrade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FinnhubWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(FinnhubWebSocketService.class);
    private static final String WEBSOCKET_URL = "wss://ws.finnhub.io";

    @Value("${finnhub.api.key:demo}")
    private String apiKey;

    @Value("${finnhub.symbols:AAPL,MSFT,TSLA,GOOGL,AMZN}")
    private String symbols;

    @Value("${finnhub.websocket.enabled:true}")
    private boolean websocketEnabled;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private LatencyMetrics latencyMetrics;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private WebSocketClient webSocketClient;

    @EventListener(ApplicationReadyEvent.class)
    public void startWebSocket() {
        if (!websocketEnabled) {
            logger.info("Finnhub WebSocket is disabled");
            return;
        }

        if ("demo".equals(apiKey)) {
            logger.warn("Using demo API key for Finnhub. Please set finnhub.api.key property");
        }

        connectWebSocket();
    }

    private void connectWebSocket() {
        try {
            URI serverUri = URI.create(WEBSOCKET_URL + "?token=" + apiKey);
            
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    logger.info("Finnhub WebSocket connected successfully");
                    connected.set(true);
                    subscribeToSymbols();
                }

                @Override
                public void onMessage(String message) {
                    long messageReceiveTime = System.nanoTime();
                    handleMessage(message, messageReceiveTime);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.warn("Finnhub WebSocket closed: {} - {}", code, reason);
                    connected.set(false);
                    
                    if (websocketEnabled) {
                        latencyMetrics.incrementWebSocketReconnections();
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    logger.error("Finnhub WebSocket error: {}", ex.getMessage());
                    connected.set(false);
                }
            };

            webSocketClient.connect();
            
        } catch (Exception e) {
            logger.error("Failed to connect to Finnhub WebSocket: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void subscribeToSymbols() {
        List<String> symbolList = Arrays.asList(symbols.split(","));
        
        for (String symbol : symbolList) {
            String trimmedSymbol = symbol.trim();
            String subscribeMessage = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", trimmedSymbol);
            
            try {
                webSocketClient.send(subscribeMessage);
                logger.info("Subscribed to symbol: {}", trimmedSymbol);
            } catch (Exception e) {
                logger.error("Failed to subscribe to symbol {}: {}", trimmedSymbol, e.getMessage());
            }
        }
    }

    private void handleMessage(String message, long messageReceiveTime) {
        Timer.Sample dataSourceSample = latencyMetrics.startDataSourceTimer();
        
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            
            if (rootNode.has("type") && "trade".equals(rootNode.get("type").asText())) {
                JsonNode dataNode = rootNode.get("data");
                
                if (dataNode != null && dataNode.isArray()) {
                    for (JsonNode tradeNode : dataNode) {
                        FinnhubTrade trade = objectMapper.treeToValue(tradeNode, FinnhubTrade.class);
                        if (trade != null) {
                            processTrade(trade, messageReceiveTime);
                        }
                    }
                }
                latencyMetrics.recordDataSourceLatency(dataSourceSample);
            } else if (rootNode.has("type") && "ping".equals(rootNode.get("type").asText())) {
                // Handle ping/heartbeat
                logger.debug("Received ping from Finnhub WebSocket");
                latencyMetrics.recordDataSourceLatency(dataSourceSample);
            }
            
        } catch (Exception e) {
            latencyMetrics.recordDataSourceLatency(dataSourceSample);
            logger.error("Error processing Finnhub message: {}", e.getMessage());
            logger.debug("Message content: {}", message);
        }
    }
    
    // Backward compatibility method
    private void handleMessage(String message) {
        handleMessage(message, System.nanoTime());
    }

    private void processTrade(FinnhubTrade trade, long messageReceiveTime) {
        try {
            // Time serialization
            Timer.Sample serializationSample = latencyMetrics.startSerializationTimer();
            
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(trade.getTimestamp()));
            long timestampMillis = System.currentTimeMillis();
            
            String tradeData = objectMapper.writeValueAsString(trade);
            String aeronMessage = String.format(
                "{\"timestamp\":\"%s\",\"timestampMillis\":%d,\"type\":\"FINNHUB_TRADE\",\"data\":%s}", 
                timestamp, timestampMillis, tradeData
            );
            
            latencyMetrics.recordSerializationLatency(serializationSample);
            
            // Time publication
            Timer.Sample publicationSample = latencyMetrics.startPublicationTimer();
            
            boolean sent = messageProducer.sendMessage(aeronMessage);
            
            if (sent) {
                latencyMetrics.recordPublicationLatency(publicationSample);
                latencyMetrics.incrementMessagesPublished();
                latencyMetrics.recordEndToEndLatency(messageReceiveTime);
                
                long latencyMicros = (System.nanoTime() - messageReceiveTime) / 1000;
                logger.info("Sent trade data for {}: ${} (vol: {}) - WebSocket->Aeron latency: {}μs", 
                        trade.getSymbol(), trade.getPrice(), trade.getVolume(), latencyMicros);
            } else {
                latencyMetrics.recordPublicationLatency(publicationSample);
                latencyMetrics.incrementPublicationErrors();
                logger.warn("Failed to send trade data for {}", trade.getSymbol());
            }
            
        } catch (Exception e) {
            latencyMetrics.incrementPublicationErrors();
            logger.error("Error processing trade for {}: {}", trade.getSymbol(), e.getMessage());
        }
    }
    
    // Backward compatibility method
    private void processTrade(FinnhubTrade trade) {
        processTrade(trade, System.nanoTime());
    }

    private void scheduleReconnect() {
        if (!websocketEnabled) return;
        
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds before reconnecting
                logger.info("Attempting to reconnect to Finnhub WebSocket...");
                connectWebSocket();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void stop() {
        websocketEnabled = false;
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    public boolean isConnected() {
        return connected.get();
    }
}
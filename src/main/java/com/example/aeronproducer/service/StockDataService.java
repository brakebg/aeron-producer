package com.example.aeronproducer.service;

import com.example.aeronproducer.metrics.LatencyMetrics;
import com.example.aeronproducer.model.StockQuote;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StockDataService {

    private static final Logger logger = LoggerFactory.getLogger(StockDataService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AlphaVantageService alphaVantageService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private LatencyMetrics latencyMetrics;

    @Value("${alphavantage.stocks.symbols:AAPL,GOOGL,MSFT}")
    private String stockSymbols;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger requestCount = new AtomicInteger(0);

    public void fetchAndSendStockData() {
        List<String> symbols = Arrays.asList(stockSymbols.split(","));
        
        symbols.forEach(symbol -> {
            String trimmedSymbol = symbol.trim();
            
            if (shouldFetchSymbol()) {
                fetchStockQuote(trimmedSymbol);
            } else {
                logger.warn("Skipping {} due to rate limiting (5 requests/minute)", trimmedSymbol);
            }
        });
    }

    private boolean shouldFetchSymbol() {
        int count = requestCount.incrementAndGet();
        if (count > 5) {
            requestCount.set(0);
            return false;
        }
        return true;
    }

    private void fetchStockQuote(String symbol) {
        long startNanos = System.nanoTime();
        Timer.Sample dataSourceSample = latencyMetrics.startDataSourceTimer();
        
        latencyMetrics.incrementApiCalls();
        
        alphaVantageService.getGlobalQuote(symbol)
            .subscribe(
                stockQuote -> {
                    latencyMetrics.recordDataSourceLatency(dataSourceSample);
                    
                    if (stockQuote != null) {
                        sendStockData(stockQuote, startNanos);
                    } else {
                        logger.warn("Received null stock quote for symbol: {}", symbol);
                    }
                },
                error -> {
                    latencyMetrics.recordDataSourceLatency(dataSourceSample);
                    logger.error("Error fetching stock data for {}: {}", symbol, error.getMessage());
                    sendErrorMessage(symbol, error.getMessage());
                }
            );
    }

    private void sendStockData(StockQuote stockQuote, long startNanos) {
        try {
            // Time serialization
            Timer.Sample serializationSample = latencyMetrics.startSerializationTimer();
            
            String timestamp = LocalDateTime.now().format(formatter);
            long timestampMillis = System.currentTimeMillis();
            
            String jsonMessage = objectMapper.writeValueAsString(stockQuote);
            
            String message = String.format(
                "{\"timestamp\":\"%s\",\"timestampMillis\":%d,\"type\":\"STOCK_QUOTE\",\"data\":%s}", 
                timestamp, timestampMillis, jsonMessage);
            
            latencyMetrics.recordSerializationLatency(serializationSample);
            
            // Time publication
            Timer.Sample publicationSample = latencyMetrics.startPublicationTimer();
            
            boolean sent = messageProducer.sendMessage(message);
            
            if (sent) {
                latencyMetrics.recordPublicationLatency(publicationSample);
                latencyMetrics.incrementMessagesPublished();
                latencyMetrics.recordEndToEndLatency(startNanos);
                
                logger.info("Sent stock data for {}: ${} ({}) - E2E latency: {}μs", 
                        stockQuote.getSymbol(), stockQuote.getPrice(), stockQuote.getChangePercent(),
                        (System.nanoTime() - startNanos) / 1000);
            } else {
                latencyMetrics.recordPublicationLatency(publicationSample);
                latencyMetrics.incrementPublicationErrors();
                logger.warn("Failed to send stock data for {}", stockQuote.getSymbol());
            }
        } catch (Exception e) {
            latencyMetrics.incrementPublicationErrors();
            logger.error("Error serializing stock data for {}: {}", stockQuote.getSymbol(), e.getMessage());
        }
    }
    
    // Overload for backward compatibility
    private void sendStockData(StockQuote stockQuote) {
        sendStockData(stockQuote, System.nanoTime());
    }

    private void sendErrorMessage(String symbol, String error) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String message = String.format("{\"timestamp\":\"%s\",\"type\":\"ERROR\",\"symbol\":\"%s\",\"error\":\"%s\"}", 
                    timestamp, symbol, error);
            
            messageProducer.sendMessage(message);
        } catch (Exception e) {
            logger.error("Error sending error message: {}", e.getMessage());
        }
    }

    public void fetchSingleStock(String symbol) {
        fetchStockQuote(symbol.trim());
    }
}
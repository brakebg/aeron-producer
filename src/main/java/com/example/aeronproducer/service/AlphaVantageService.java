package com.example.aeronproducer.service;

import com.example.aeronproducer.model.StockQuote;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class AlphaVantageService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageService.class);
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    @Value("${alphavantage.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AlphaVantageService() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Mono<StockQuote> getGlobalQuote(String symbol) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("function", "GLOBAL_QUOTE")
                        .queryParam("symbol", symbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .map(this::parseGlobalQuote)
                .doOnNext(quote -> logger.debug("Fetched quote for {}: {}", symbol, quote))
                .doOnError(error -> logger.error("Error fetching quote for {}: {}", symbol, error.getMessage()));
    }

    private StockQuote parseGlobalQuote(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode globalQuote = root.get("Global Quote");
            
            if (globalQuote == null) {
                logger.warn("No Global Quote found in response: {}", jsonResponse);
                return null;
            }
            
            return objectMapper.treeToValue(globalQuote, StockQuote.class);
        } catch (Exception e) {
            logger.error("Error parsing stock quote response: {}", e.getMessage());
            return null;
        }
    }

    public Mono<String> getIntraday(String symbol, String interval) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("function", "TIME_SERIES_INTRADAY")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", interval)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> logger.error("Error fetching intraday data for {}: {}", symbol, error.getMessage()));
    }
}
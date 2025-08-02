package com.example.aeronproducer.scheduler;

import com.example.aeronproducer.service.MessageProducer;
import com.example.aeronproducer.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@EnableScheduling
public class MessageScheduler {

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private StockDataService stockDataService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(fixedRate = 5000)
    @ConditionalOnProperty(name = "aeron.producer.auto-send", havingValue = "true", matchIfMissing = false)
    public void sendPeriodicMessage() {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = "Periodic message at " + timestamp;
        messageProducer.sendMessage(message);
    }

    @Scheduled(fixedDelayString = "${alphavantage.stocks.fetch-interval:60000}")
    @ConditionalOnProperty(name = "aeron.producer.stock-data", havingValue = "true", matchIfMissing = false)
    public void fetchAndSendStockData() {
        stockDataService.fetchAndSendStockData();
    }
}
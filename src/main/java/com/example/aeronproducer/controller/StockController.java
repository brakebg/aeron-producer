package com.example.aeronproducer.controller;

import com.example.aeronproducer.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    @Autowired
    private StockDataService stockDataService;

    @GetMapping("/fetch/{symbol}")
    public ResponseEntity<Map<String, Object>> fetchStock(@PathVariable String symbol) {
        try {
            stockDataService.fetchSingleStock(symbol);
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Stock data fetch initiated for " + symbol
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false, 
                "error", "Failed to fetch stock data: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchAllStocks() {
        try {
            stockDataService.fetchAndSendStockData();
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Stock data fetch initiated for all configured symbols"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false, 
                "error", "Failed to fetch stock data: " + e.getMessage()
            ));
        }
    }
}
package com.example.aeronproducer.controller;

import com.example.aeronproducer.service.FinnhubWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/websocket")
public class WebSocketController {

    @Autowired
    private FinnhubWebSocketService finnhubWebSocketService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean connected = finnhubWebSocketService.isConnected();
        
        return ResponseEntity.ok(Map.of(
            "connected", connected,
            "service", "Finnhub WebSocket",
            "status", connected ? "Connected" : "Disconnected"
        ));
    }

    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restartWebSocket() {
        try {
            finnhubWebSocketService.stop();
            Thread.sleep(2000); // Wait 2 seconds
            finnhubWebSocketService.startWebSocket();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "WebSocket restart initiated"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to restart WebSocket: " + e.getMessage()
            ));
        }
    }
}
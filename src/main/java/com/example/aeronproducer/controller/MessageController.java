package com.example.aeronproducer.controller;

import com.example.aeronproducer.service.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageProducer messageProducer;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Message cannot be empty"));
        }

        boolean sent = messageProducer.sendMessage(message);
        
        if (sent) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Message sent successfully"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Failed to send message"));
        }
    }

    @GetMapping("/send/{message}")
    public ResponseEntity<Map<String, Object>> sendMessageGet(@PathVariable String message) {
        boolean sent = messageProducer.sendMessage(message);
        
        if (sent) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Message sent successfully"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Failed to send message"));
        }
    }
}
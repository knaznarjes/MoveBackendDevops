package com.move.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @GetMapping("/debug-auth-headers")
    public ResponseEntity<Map<String, Object>> debugAuthHeaders(@RequestHeader Map<String, String> headers) {
        logger.debug("Debug auth headers request received");

        Map<String, Object> response = new HashMap<>();
        response.put("headers", headers);
        response.put("message", "Gateway debug info");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-routing")
    public ResponseEntity<String> testRouting() {
        return ResponseEntity.ok("API Gateway routing is working");
    }

    @GetMapping("/auth-health")
    public ResponseEntity<String> authHealth() {
        return ResponseEntity.ok("API Gateway auth system is operational");
    }
}
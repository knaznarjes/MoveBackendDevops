package com.move.communitynotificationservice.controller;

import com.move.communitynotificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final NotificationService notificationService;

    // Constructeur manuel (équivalent à @RequiredArgsConstructor de Lombok)
    public WebSocketController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @MessageMapping("/notifications/subscribe")
    public void subscribeToNotifications(@Payload Map<String, String> payload,
                                         SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        log.info("Utilisateur {} s'abonne aux notifications WebSocket", userId);

        // Stocker l'userId dans la session WebSocket
        headerAccessor.getSessionAttributes().put("userId", userId);
    }

    @SubscribeMapping("/topic/notifications/{userId}")
    public void handleSubscription(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Nouvelle souscription WebSocket pour la session: {}", sessionId);
    }




}

package com.move.communitynotificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("Nouvelle connexion WebSocket établie. Session ID: {}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // Récupérer l'userId depuis les attributs de session si disponible
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");

        log.info("Connexion WebSocket fermée. Session ID: {}, User ID: {}", sessionId, userId);

        // Ici vous pourriez implémenter une logique de nettoyage si nécessaire
        // Par exemple, mettre à jour le statut de présence de l'utilisateur
    }
}
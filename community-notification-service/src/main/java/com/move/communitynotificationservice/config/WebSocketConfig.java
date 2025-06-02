package com.move.communitynotificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Bean(name = "customWebSocketTaskScheduler")
    public TaskScheduler customWebSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Configuration du message broker avec optimisations
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 20000}) // heartbeat pour maintenir la connexion
                .setTaskScheduler(customWebSocketTaskScheduler()); // Utiliser le scheduler configuré

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Configuration des endpoints WebSocket avec gestion des origines
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS()
                .setHeartbeatTime(25000) // Heartbeat pour SockJS
                .setDisconnectDelay(5000)
                .setStreamBytesLimit(128 * 1024)
                .setHttpMessageCacheSize(1000)
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");

        // Endpoint sans SockJS pour les clients natifs
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        return handleConnect(accessor, message);
                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        return handleSubscribe(accessor, message);
                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        return handleDisconnect(accessor, message);
                    }
                }
                return message;
            }
        });

        // Configuration du thread pool pour les messages entrants
        registration.taskExecutor().corePoolSize(4);
        registration.taskExecutor().maxPoolSize(8);
        registration.taskExecutor().queueCapacity(1000);
    }

    private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
        try {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                log.error("Token manquant ou format invalide dans la connexion WebSocket");
                throw new IllegalArgumentException("Token manquant ou format invalide");
            }

            token = token.substring(7);

            if (validateToken(token)) {
                String userId = extractUserId(token);
                String username = extractUsername(token);

                if (userId != null) {
                    accessor.setUser(() -> userId);
                    accessor.getSessionAttributes().put("userId", userId);
                    accessor.getSessionAttributes().put("username", username);
                    accessor.getSessionAttributes().put("authenticated", true);

                    log.info("Utilisateur WebSocket authentifié: {} ({})", username, userId);
                } else {
                    throw new IllegalArgumentException("ID utilisateur introuvable dans le token");
                }
            } else {
                log.error("Token WebSocket invalide");
                throw new IllegalArgumentException("Token invalide");
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'authentification WebSocket: {}", e.getMessage());
            throw new IllegalArgumentException("Échec de l'authentification: " + e.getMessage());
        }

        return message;
    }

    private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();
        String userId = (String) accessor.getSessionAttributes().get("userId");

        if (destination != null && userId != null) {
            // Vérifier que l'utilisateur ne s'abonne qu'à ses propres notifications
            if (destination.startsWith("/topic/notifications/") || destination.startsWith("/queue/notifications/")) {
                String destinationUserId = extractUserIdFromDestination(destination);
                if (destinationUserId != null && !destinationUserId.equals(userId)) {
                    log.warn("Tentative d'abonnement non autorisé à {} par l'utilisateur {}", destination, userId);
                    throw new IllegalArgumentException("Abonnement non autorisé");
                }
            }

            log.debug("Abonnement autorisé: {} pour l'utilisateur {}", destination, userId);
        }

        return message;
    }

    private Message<?> handleDisconnect(StompHeaderAccessor accessor, Message<?> message) {
        String userId = (String) accessor.getSessionAttributes().get("userId");
        String sessionId = accessor.getSessionId();

        log.info("Déconnexion WebSocket - Session: {}, Utilisateur: {}", sessionId, userId);

        return message;
    }

    private String extractUserIdFromDestination(String destination) {
        if (destination == null) return null;

        String[] parts = destination.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("notifications".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims != null && !claims.getExpiration().before(new java.util.Date());
        } catch (Exception e) {
            log.error("Erreur validation token WebSocket: {}", e.getMessage());
            return false;
        }
    }

    private String extractUserId(String token) {
        try {
            Claims claims = parseClaims(token);

            // Priorité au userId exact du token
            String userId = null;

            if (claims.containsKey("userId")) {
                userId = claims.get("userId", String.class);
            } else if (claims.containsKey("id")) {
                userId = claims.get("id", String.class);
            } else {
                userId = claims.getSubject(); // Fallback sur le subject
            }

            log.debug("UserId extrait du token WebSocket: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Erreur extraction userId du token WebSocket: {}", e.getMessage());
            return null;
        }
    }

    private String extractUsername(String token) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject(); // Le subject contient généralement le username
            log.debug("Username extrait du token WebSocket: {}", username);
            return username;
        } catch (Exception e) {
            log.error("Erreur extraction username du token WebSocket: {}", e.getMessage());
            return null;
        }
    }

    private Claims parseClaims(String token) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);

            if (keyBytes.length < 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }

            Key key = Keys.hmacShaKeyFor(keyBytes);

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Erreur parsing claims WebSocket: {}", e.getMessage());
            throw e;
        }
    }
}
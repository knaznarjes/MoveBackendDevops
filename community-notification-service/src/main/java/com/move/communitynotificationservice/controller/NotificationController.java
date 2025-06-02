package com.move.communitynotificationservice.controller;

import com.move.communitynotificationservice.model.Notification;
import com.move.communitynotificationservice.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    @PostMapping("/test")
    public ResponseEntity<String> sendTestNotification(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");
        String message = payload.getOrDefault("message", "Notification test");
        notificationService.createNotification(userId, message);
        return ResponseEntity.ok("Notification envoyée !");
    }
    /**
     * Récupère toutes les notifications d'un utilisateur
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getNotifications(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Récupération des notifications pour l'utilisateur: {}", userId);

            // Vérifier l'authentification
            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé aux notifications"));
            }

            List<Notification> notifications = notificationService.getNotifications(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications);
            response.put("total", notifications.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètre utilisateur invalide"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des notifications"));
        }
    }

    /**
     * Récupère les notifications avec pagination
     */
    @GetMapping("/{userId}/paginated")
    public ResponseEntity<?> getNotificationsPaginated(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            log.info("Récupération paginée des notifications pour l'utilisateur: {} (page: {}, size: {})", userId, page, size);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            // Validation des paramètres de pagination
            if (page < 0 || size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Paramètres de pagination invalides"));
            }

            Page<Notification> notificationsPage = notificationService.getNotificationsPaginated(userId, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notificationsPage.getContent());
            response.put("currentPage", notificationsPage.getNumber());
            response.put("totalPages", notificationsPage.getTotalPages());
            response.put("totalElements", notificationsPage.getTotalElements());
            response.put("size", notificationsPage.getSize());
            response.put("hasNext", notificationsPage.hasNext());
            response.put("hasPrevious", notificationsPage.hasPrevious());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Paramètres invalides pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètres invalides"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération paginée des notifications pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des notifications"));
        }
    }

    /**
     * Récupère les notifications non lues d'un utilisateur
     */
    @GetMapping("/{userId}/unread")
    public ResponseEntity<?> getUnreadNotifications(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Récupération des notifications non lues pour l'utilisateur: {}", userId);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            List<Notification> notifications = notificationService.getUnreadNotifications(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications);
            response.put("count", notifications.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètre utilisateur invalide"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications non lues pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des notifications"));
        }
    }

    /**
     * Récupère le nombre de notifications non lues
     */
    @GetMapping("/{userId}/count")
    public ResponseEntity<?> getUnreadCount(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Récupération du nombre de notifications non lues pour l'utilisateur: {}", userId);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            long count = notificationService.getUnreadCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du nombre de notifications non lues pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération du compte"));
        }
    }

    /**
     * Récupère une notification spécifique par son ID
     */
    @GetMapping("/notification/{notificationId}")
    public ResponseEntity<?> getNotificationById(@PathVariable String notificationId, HttpServletRequest request) {
        try {
            log.info("Récupération de la notification: {}", notificationId);

            String userId = extractUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            Optional<Notification> notificationOpt = notificationService.getNotificationById(notificationId);

            if (notificationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Notification notification = notificationOpt.get();
            if (!notification.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé à cette notification"));
            }

            return ResponseEntity.ok(notification);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour notificationId {}: {}", notificationId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("ID de notification invalide"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la notification {}: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération de la notification"));
        }
    }

    /**
     * Marque une notification comme lue
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId, HttpServletRequest request) {
        try {
            log.info("Marquage de la notification comme lue: {}", notificationId);

            String userId = extractUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            // Vérifier d'abord que la notification existe et appartient à l'utilisateur
            Optional<Notification> notificationOpt = notificationService.getNotificationById(notificationId);
            if (notificationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!notificationOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé à cette notification"));
            }

            Notification notification = notificationService.markAsRead(notificationId);

            Map<String, Object> response = new HashMap<>();
            response.put("notification", notification);
            response.put("success", true);
            response.put("message", "Notification marquée comme lue");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Notification non trouvée: {}", notificationId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors du marquage de la notification comme lue {}: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise à jour"));
        }
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    @PutMapping("/{userId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Marquage de toutes les notifications comme lues pour l'utilisateur: {}", userId);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            notificationService.markAllAsRead(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Toutes les notifications ont été marquées comme lues");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètre utilisateur invalide"));
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications comme lues pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la mise à jour"));
        }
    }

    /**
     * Supprime une notification spécifique
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable String notificationId, HttpServletRequest request) {
        try {
            log.info("Suppression de la notification: {}", notificationId);

            String userId = extractUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            // Récupérer la notification pour vérifier la propriété
            Optional<Notification> notificationOpt = notificationService.getNotificationById(notificationId);
            if (notificationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!notificationOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé à cette notification"));
            }

            notificationService.deleteNotification(notificationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification supprimée avec succès");
            response.put("notificationId", notificationId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Notification non trouvée pour suppression: {}", notificationId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la notification {}: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression"));
        }
    }

    /**
     * Supprime toutes les notifications d'un utilisateur
     */
    @DeleteMapping("/{userId}/all")
    public ResponseEntity<?> deleteAllNotifications(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Suppression de toutes les notifications pour l'utilisateur: {}", userId);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            notificationService.deleteAllNotifications(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Toutes les notifications ont été supprimées");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètre utilisateur invalide"));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de toutes les notifications pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression"));
        }
    }

    /**
     * Récupère les statistiques de notifications pour un utilisateur
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getNotificationStats(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Récupération des statistiques de notifications pour l'utilisateur: {}", userId);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNotifications", notificationService.getNotifications(userId).size());
            stats.put("unreadCount", notificationService.getUnreadCount(userId));

            // Statistiques par type
            Map<String, Long> typeStats = new HashMap<>();
            for (NotificationService.NotificationType type : NotificationService.NotificationType.values()) {
                long count = notificationService.getTotalNotificationsByType(userId, type.getValue());
                typeStats.put(type.getValue(), count);
            }
            stats.put("byType", typeStats);
            stats.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(stats);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètre utilisateur invalide"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des statistiques"));
        }
    }

    /**
     * Nettoyage des notifications lues pour un utilisateur
     */
    @DeleteMapping("/{userId}/cleanup-read")
    public ResponseEntity<?> cleanupReadNotifications(@PathVariable String userId, HttpServletRequest request) {
        try {
            log.info("Nettoyage des notifications lues pour l'utilisateur: {}", userId);

            if (!isAuthorized(userId, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            notificationService.cleanupReadNotifications(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notifications lues supprimées avec succès");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Paramètre invalide pour userId {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Paramètre utilisateur invalide"));
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des notifications lues pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du nettoyage"));
        }
    }

    // =============== MÉTHODES UTILITAIRES PRIVÉES ===============

    /**
     * Vérifie si l'utilisateur est autorisé à accéder aux notifications
     */
    private boolean isAuthorized(String userId, HttpServletRequest request) {
        String authenticatedUserId = extractUserId(request);
        return authenticatedUserId != null && userId.equals(authenticatedUserId);
    }

    /**
     * Extrait l'ID utilisateur de la requête
     */
    private String extractUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }

    /**
     * Crée une réponse d'erreur standardisée
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now());
        return error;
    }

    /**
     * Crée une réponse de succès standardisée
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
    @GetMapping("/api/notifications/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String userId) {
        List<Notification> unread = notificationService.getUnreadNotificationsByUser(userId);
        return ResponseEntity.ok(unread);
    }

}
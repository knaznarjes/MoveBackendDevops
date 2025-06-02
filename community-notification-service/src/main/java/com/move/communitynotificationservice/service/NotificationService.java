package com.move.communitynotificationservice.service;

import com.move.communitynotificationservice.model.Comment;
import com.move.communitynotificationservice.model.Notification;
import com.move.communitynotificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Enum pour les types de notifications
    public enum NotificationType {
        COMMENT("COMMENT"),
        LIKE("LIKE"),
        FOLLOW("FOLLOW"),
        CONTENT_CREATED("CONTENT_CREATED"),
        CONTENT_SHARED("CONTENT_SHARED"),
        MENTION("MENTION"),
        SYSTEM("SYSTEM");

        private final String value;

        NotificationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Méthode principale pour créer une notification
     */
    public void createNotification(String receiverId, String message) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);

        notificationRepository.save(notification);

        log.info("🔔 Notification créée pour {} : {}", receiverId, message);

        // Envoi en temps réel via WebSocket
        messagingTemplate.convertAndSend("/topic/notifications/" + receiverId, notification);
    }


    public Notification createNotification(String userId, String message, String type, String sourceId, String metadata) {
        return createNotification(userId, message, type, sourceId, null, metadata);
    }

    public Notification createNotification(String userId, String message, String type, String sourceId, String sourceName, String metadata) {
        // Validation des paramètres obligatoires
        if (userId == null || userId.trim().isEmpty()) {
            log.error("Tentative de création de notification avec userId null ou vide");
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        if (message == null || message.trim().isEmpty()) {
            log.error("Tentative de création de notification avec message null ou vide");
            throw new IllegalArgumentException("Message ne peut pas être null ou vide");
        }

        log.info("Création d'une notification pour l'utilisateur: {} avec le message: {}", userId, message);

        try {
            Notification notification = new Notification();
            notification.setUserId(userId.trim());
            notification.setMessage(message.trim());
            notification.setType(type != null ? type.trim() : NotificationType.SYSTEM.getValue());
            notification.setSourceId(sourceId != null ? sourceId.trim() : null);
            notification.setSourceName(sourceName != null ? sourceName.trim() : null);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setMetadata(metadata);
            notification.setPriority(determinePriority(type));

            Notification saved = notificationRepository.save(notification);
            log.info("Notification sauvegardée avec l'ID: {}", saved.getId());

            // Envoyer la notification en temps réel via WebSocket de manière asynchrone
            sendRealTimeNotificationAsync(userId.trim(), saved);

            return saved;
        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification pour userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de créer la notification", e);
        }
    }

    /**
     * Détermine la priorité en fonction du type de notification
     */
    private int determinePriority(String type) {
        if (type == null) return 1;

        switch (type.toUpperCase()) {
            case "FOLLOW":
            case "MENTION":
                return 2; // Haute priorité
            case "COMMENT":
            case "LIKE":
                return 1; // Priorité normale
            case "CONTENT_CREATED":
            case "CONTENT_SHARED":
                return 1; // Priorité normale
            case "SYSTEM":
                return 2; // Haute priorité pour les notifications système
            default:
                return 1; // Priorité normale par défaut
        }
    }

    /**
     * Envoi asynchrone des notifications WebSocket
     */
    private void sendRealTimeNotificationAsync(String userId, Notification notification) {
        CompletableFuture.runAsync(() -> {
            try {
                sendRealTimeNotification(userId, notification);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi asynchrone de la notification WebSocket pour userId {}: {}",
                        userId, e.getMessage(), e);
            }
        });
    }

    /**
     * Envoi synchrone des notifications WebSocket
     */
    private void sendRealTimeNotification(String userId, Notification notification) {
        try {
            // Vérifier que le template est disponible
            if (messagingTemplate == null) {
                log.warn("SimpMessagingTemplate non disponible pour l'envoi WebSocket");
                return;
            }

            // Envoyer à l'utilisateur spécifique
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Notification envoyée via WebSocket à la destination: {}", destination);

            // Envoyer également le nombre de notifications non lues
            long unreadCount = getUnreadCount(userId);
            String countDestination = "/topic/notifications/" + userId + "/count";
            messagingTemplate.convertAndSend(countDestination, unreadCount);
            log.debug("Compteur de notifications envoyé via WebSocket à la destination: {}", countDestination);

            log.info("Notification et compteur envoyés via WebSocket à l'utilisateur: {}", userId);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification via WebSocket pour userId {}: {}",
                    userId, e.getMessage(), e);
            // Ne pas faire échouer la création de notification si l'envoi WebSocket échoue
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Récupération des notifications pour l'utilisateur: {}", userId);
        try {
            return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId.trim());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les notifications", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsPaginated(String userId, int page, int size) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Récupération paginée des notifications pour l'utilisateur: {} (page: {}, size: {})", userId, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return notificationRepository.findByUserId(userId.trim(), pageable);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération paginée des notifications pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les notifications", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Récupération des notifications non lues pour l'utilisateur: {}", userId);
        try {
            return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId.trim());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications non lues pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les notifications non lues", e);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0;
        }
        try {
            return notificationRepository.countByUserIdAndReadFalse(userId.trim());
        } catch (Exception e) {
            log.error("Erreur lors du comptage des notifications non lues pour {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            throw new IllegalArgumentException("NotificationId ne peut pas être null ou vide");
        }
        try {
            return notificationRepository.findById(notificationId.trim());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la notification {}: {}", notificationId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Notification markAsRead(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            throw new IllegalArgumentException("NotificationId ne peut pas être null ou vide");
        }

        log.info("Marquage de la notification comme lue: {}", notificationId);
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId.trim());

            if (notificationOpt.isPresent()) {
                Notification notification = notificationOpt.get();
                if (!notification.isRead()) {
                    notification.markAsRead(); // Utilise la méthode de la classe Notification
                    Notification updated = notificationRepository.save(notification);

                    // Envoyer le nouveau compte de notifications non lues de manière asynchrone
                    CompletableFuture.runAsync(() -> {
                        try {
                            long unreadCount = getUnreadCount(notification.getUserId());
                            String countDestination = "/topic/notifications/" + notification.getUserId() + "/count";
                            messagingTemplate.convertAndSend(countDestination, unreadCount);
                        } catch (Exception e) {
                            log.error("Erreur lors de l'envoi du count via WebSocket après marquage comme lu: {}", e.getMessage());
                        }
                    });

                    return updated;
                }
                return notification;
            } else {
                log.warn("Notification non trouvée avec l'ID: {}", notificationId);
                throw new RuntimeException("Notification non trouvée");
            }
        } catch (Exception e) {
            log.error("Erreur lors du marquage de la notification comme lue {}: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Impossible de marquer la notification comme lue", e);
        }
    }

    public void markAllAsRead(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Marquage de toutes les notifications comme lues pour l'utilisateur: {}", userId);
        try {
            List<Notification> notifications = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId.trim());

            if (!notifications.isEmpty()) {
                notifications.forEach(Notification::markAsRead); // Utilise la méthode de la classe
                notificationRepository.saveAll(notifications);

                // Envoyer le nouveau compte (qui devrait être 0) de manière asynchrone
                CompletableFuture.runAsync(() -> {
                    try {
                        String countDestination = "/topic/notifications/" + userId.trim() + "/count";
                        messagingTemplate.convertAndSend(countDestination, 0L);
                    } catch (Exception e) {
                        log.error("Erreur lors de l'envoi du count via WebSocket après markAllAsRead: {}", e.getMessage());
                    }
                });

                log.info("Toutes les notifications marquées comme lues pour l'utilisateur: {} ({} notifications)", userId, notifications.size());
            } else {
                log.debug("Aucune notification non lue trouvée pour l'utilisateur: {}", userId);
            }
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications comme lues pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de marquer toutes les notifications comme lues", e);
        }
    }

    public void deleteNotification(String notificationId) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            throw new IllegalArgumentException("NotificationId ne peut pas être null ou vide");
        }

        log.info("Suppression de la notification: {}", notificationId);
        try {
            Optional<Notification> notificationOpt = notificationRepository.findById(notificationId.trim());

            if (notificationOpt.isPresent()) {
                Notification notification = notificationOpt.get();
                boolean wasUnread = !notification.isRead();
                String userId = notification.getUserId();

                notificationRepository.deleteById(notificationId.trim());

                // Mettre à jour le compte si c'était une notification non lue
                if (wasUnread) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            long unreadCount = getUnreadCount(userId);
                            String countDestination = "/topic/notifications/" + userId + "/count";
                            messagingTemplate.convertAndSend(countDestination, unreadCount);
                        } catch (Exception e) {
                            log.error("Erreur lors de l'envoi du count via WebSocket après suppression: {}", e.getMessage());
                        }
                    });
                }

                log.info("Notification supprimée avec succès: {}", notificationId);
            } else {
                log.warn("Tentative de suppression d'une notification inexistante: {}", notificationId);
                throw new RuntimeException("Notification non trouvée");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la notification {}: {}", notificationId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer la notification", e);
        }
    }

    public void deleteAllNotifications(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Suppression de toutes les notifications pour l'utilisateur: {}", userId);
        try {
            long deletedCount = notificationRepository.countByUserId(userId.trim());
            notificationRepository.deleteByUserId(userId.trim());

            // Envoyer le nouveau compte (qui devrait être 0) de manière asynchrone
            CompletableFuture.runAsync(() -> {
                try {
                    String countDestination = "/topic/notifications/" + userId.trim() + "/count";
                    messagingTemplate.convertAndSend(countDestination, 0L);
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi du count via WebSocket après suppression de toutes les notifications: {}", e.getMessage());
                }
            });

            log.info("Toutes les notifications supprimées pour l'utilisateur: {} ({} notifications)", userId, deletedCount);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de toutes les notifications pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer toutes les notifications", e);
        }
    }

    // =============== MÉTHODES UTILITAIRES POUR CRÉER DIFFÉRENTS TYPES DE NOTIFICATIONS ===============

    /**
     * Crée une notification de commentaire
     */
    public void createCommentNotification(String userId, String commenterName, String contentId, String contentTitle) {
        if (!isValidNotificationData(userId, commenterName)) {
            log.warn("Paramètres manquants pour notification de commentaire: userId={}, commenterName={}", userId, commenterName);
            return;
        }

        try {
            String safeContentTitle = sanitizeContentTitle(contentTitle);
            String message = String.format("%s a commenté votre contenu '%s'", commenterName, safeContentTitle);
            String metadata = buildMetadata("contentId", contentId, "commenterName", commenterName, "contentTitle", safeContentTitle);

            createNotification(userId, message, NotificationType.COMMENT.getValue(), contentId, commenterName, metadata);
            log.info("Notification de commentaire créée pour userId: {} par: {}", userId, commenterName);
        } catch (Exception e) {
            log.error("Erreur lors de la création de notification de commentaire pour userId {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Crée une notification de like
     */
    public void createLikeNotification(String userId, String likerName, String contentId, String contentTitle) {
        if (!isValidNotificationData(userId, likerName)) {
            log.warn("Paramètres manquants pour notification de like: userId={}, likerName={}", userId, likerName);
            return;
        }

        try {
            String safeContentTitle = sanitizeContentTitle(contentTitle);
            String message = String.format("%s a aimé votre contenu '%s'", likerName, safeContentTitle);
            String metadata = buildMetadata("contentId", contentId, "likerName", likerName, "contentTitle", safeContentTitle);

            createNotification(userId, message, NotificationType.LIKE.getValue(), contentId, likerName, metadata);
            log.info("Notification de like créée pour userId: {} par: {}", userId, likerName);
        } catch (Exception e) {
            log.error("Erreur lors de la création de notification de like pour userId {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Crée une notification de follow
     */
    public void createFollowNotification(String userId, String followerId, String followerName) {
        if (!isValidNotificationData(userId, followerName) || followerId == null || followerId.trim().isEmpty()) {
            log.warn("Paramètres manquants pour notification de follow: userId={}, followerId={}, followerName={}",
                    userId, followerId, followerName);
            return;
        }

        try {
            String message = String.format("%s a commencé à vous suivre", followerName);
            String metadata = buildMetadata("followerId", followerId, "followerName", followerName);

            createNotification(userId, message, NotificationType.FOLLOW.getValue(), followerId, followerName, metadata);
            log.info("Notification de follow créée pour userId: {} par: {}", userId, followerName);
        } catch (Exception e) {
            log.error("Erreur lors de la création de notification de follow pour userId {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Crée une notification de nouveau contenu
     */
    public void createContentNotification(String userId, String creatorName, String contentId, String contentTitle) {
        if (!isValidNotificationData(userId, creatorName)) {
            log.warn("Paramètres manquants pour notification de contenu: userId={}, creatorName={}", userId, creatorName);
            return;
        }

        try {
            String safeContentTitle = sanitizeContentTitle(contentTitle, "nouveau contenu");
            String message = String.format("%s a publié un nouveau contenu: '%s'", creatorName, safeContentTitle);
            String metadata = buildMetadata("contentId", contentId, "creatorName", creatorName, "contentTitle", safeContentTitle);

            createNotification(userId, message, NotificationType.CONTENT_CREATED.getValue(), contentId, creatorName, metadata);
            log.info("Notification de nouveau contenu créée pour userId: {} par: {}", userId, creatorName);
        } catch (Exception e) {
            log.error("Erreur lors de la création de notification de contenu pour userId {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Crée une notification de mention
     */
    public void createMentionNotification(String userId, String mentionerName, String contentId, String contentTitle) {
        if (!isValidNotificationData(userId, mentionerName)) {
            log.warn("Paramètres manquants pour notification de mention: userId={}, mentionerName={}", userId, mentionerName);
            return;
        }

        try {
            String safeContentTitle = sanitizeContentTitle(contentTitle);
            String message = String.format("%s vous a mentionné dans '%s'", mentionerName, safeContentTitle);
            String metadata = buildMetadata("contentId", contentId, "mentionerName", mentionerName, "contentTitle", safeContentTitle);

            createNotification(userId, message, NotificationType.MENTION.getValue(), contentId, mentionerName, metadata);
            log.info("Notification de mention créée pour userId: {} par: {}", userId, mentionerName);
        } catch (Exception e) {
            log.error("Erreur lors de la création de notification de mention pour userId {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Crée une notification de partage de contenu
     */
    public void createContentSharedNotification(String userId, String sharerName, String contentId, String contentTitle) {
        if (!isValidNotificationData(userId, sharerName)) {
            log.warn("Paramètres manquants pour notification de partage: userId={}, sharerName={}", userId, sharerName);
            return;
        }

        try {
            String safeContentTitle = sanitizeContentTitle(contentTitle, "votre contenu");
            String message = String.format("%s a partagé votre contenu '%s'", sharerName, safeContentTitle);
            String metadata = buildMetadata("contentId", contentId, "sharerName", sharerName, "contentTitle", safeContentTitle);

            createNotification(userId, message, NotificationType.CONTENT_SHARED.getValue(), contentId, sharerName, metadata);
            log.info("Notification de partage créée pour userId: {} par: {}", userId, sharerName);
        } catch (Exception e) {
            log.error("Erreur lors de la création de notification de partage pour userId {}: {}", userId, e.getMessage(), e);
        }
    }

    // =============== MÉTHODES UTILITAIRES PRIVÉES ===============

    /**
     * Valide les données de base pour une notification
     */
    private boolean isValidNotificationData(String userId, String actorName) {
        return userId != null && !userId.trim().isEmpty() &&
                actorName != null && !actorName.trim().isEmpty();
    }

    /**
     * Sécurise le titre du contenu
     */
    private String sanitizeContentTitle(String contentTitle) {
        return sanitizeContentTitle(contentTitle, "un contenu");
    }

    private String sanitizeContentTitle(String contentTitle, String defaultValue) {
        if (contentTitle == null || contentTitle.trim().isEmpty()) {
            return defaultValue;
        }

        String sanitized = contentTitle.trim();
        // Limiter la longueur pour éviter des messages trop longs
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 47) + "...";
        }

        return sanitized;
    }

    /**
     * Construit les métadonnées JSON de manière sécurisée
     */
    private String buildMetadata(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Les arguments doivent être fournis par paires clé-valeur");
        }

        StringBuilder metadata = new StringBuilder("{");
        boolean first = true;

        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];

            if (key != null && value != null) {
                if (!first) {
                    metadata.append(",");
                }
                metadata.append("\"").append(escapeJson(key)).append("\":\"")
                        .append(escapeJson(value)).append("\"");
                first = false;
            }
        }

        metadata.append("}");
        return metadata.toString();
    }

    /**
     * Échappe les caractères spéciaux pour JSON
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // =============== MÉTHODES DE STATISTIQUES ET REPORTING ===============

    @Transactional(readOnly = true)
    public long getTotalNotificationsByType(String userId, String type) {
        if (userId == null || userId.trim().isEmpty()) {
            return 0;
        }
        try {
            return notificationRepository.countByUserIdAndType(userId.trim(), type);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des notifications par type pour {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(String userId, LocalDateTime since) {
        if (userId == null || userId.trim().isEmpty() || since == null) {
            throw new IllegalArgumentException("Paramètres invalides pour récupération des notifications récentes");
        }
        try {
            return notificationRepository.findRecentNotifications(userId.trim(), since);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications récentes pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les notifications récentes", e);
        }
    }

    // =============== MÉTHODES DE NETTOYAGE ===============

    @Transactional
    public void cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        log.info("Nettoyage des notifications antérieures à: {}", cutoffDate);

        try {
            notificationRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("Nettoyage des anciennes notifications terminé");
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des anciennes notifications: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors du nettoyage des anciennes notifications", e);
        }
    }

    @Transactional
    public void cleanupReadNotifications(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Nettoyage des notifications lues pour l'utilisateur: {}", userId);

        try {
            notificationRepository.deleteByUserIdAndReadTrue(userId.trim());
            log.info("Nettoyage des notifications lues terminé pour l'utilisateur: {}", userId);
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des notifications lues pour {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors du nettoyage des notifications lues", e);
        }
    }
    public List<Notification> getUnreadNotificationsByUser(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }


    public void createAndSendNotification(String targetUserId, String message, String contentId) {
        Notification notification = new Notification();
        notification.setUserId(targetUserId);
        notification.setMessage(message);
        notification.setContentId(contentId);
        notification.setRead(false);
        notification.setTimestamp(LocalDateTime.now());

        notificationRepository.save(notification);

        // ⚡ Diffuser via WebSocket STOMP
        messagingTemplate.convertAndSend("/topic/notifications/" + targetUserId, notification);
    }

}
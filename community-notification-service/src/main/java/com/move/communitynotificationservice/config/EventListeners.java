package com.move.communitynotificationservice.config;

import com.move.communitynotificationservice.model.Comment;
import com.move.communitynotificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class EventListeners {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log = LoggerFactory.getLogger(EventListeners.class);
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RestTemplate restTemplate;

    // ✅ SUPPRIMÉ - Ce listener sera géré par ContentEventListener
    // @RabbitListener(queues = "comment.created")
    // public void onCommentCreated(Comment comment) { ... }

    @RabbitListener(queues = "like.created")
    public void onLikeCreated(Map<String, Object> likeEvent) {
        log.info("Réception d'un événement de like créé: {}", likeEvent);

        // Traitement asynchrone
        CompletableFuture.runAsync(() -> {
            try {
                processLikeCreated(likeEvent);
            } catch (Exception e) {
                log.error("Erreur lors du traitement asynchrone du like: {}", e.getMessage(), e);
            }
        });
    }

    private void processLikeCreated(Map<String, Object> likeEvent) {
        try {
            String likerId = extractString(likeEvent, "userId");
            String contentId = extractString(likeEvent, "contentId");

            if (likerId == null || contentId == null) {
                log.warn("Données manquantes dans l'événement like: userId={}, contentId={}", likerId, contentId);
                return;
            }

            String contentOwnerId = getContentOwnerId(contentId);

            if (contentOwnerId != null && !contentOwnerId.equals(likerId)) {
                // Récupérer les informations en parallèle
                CompletableFuture<String> likerNameFuture = CompletableFuture.supplyAsync(() ->
                        getUserName(likerId));
                CompletableFuture<String> contentTitleFuture = CompletableFuture.supplyAsync(() ->
                        getContentTitle(contentId));

                String likerName = likerNameFuture.join();
                String contentTitle = contentTitleFuture.join();

                notificationService.createLikeNotification(
                        contentOwnerId,
                        likerName,
                        contentId,
                        contentTitle
                );

                log.info("Notification de like créée avec succès pour l'owner: {}", contentOwnerId);
            } else {
                log.info("Pas de notification créée - l'utilisateur like son propre contenu ou owner introuvable");
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement like créé: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "follow.created")
    public void onFollowCreated(Map<String, Object> followEvent) {
        log.info("Réception d'un événement de follow créé: {}", followEvent);

        // Traitement asynchrone
        CompletableFuture.runAsync(() -> {
            try {
                processFollowCreated(followEvent);
            } catch (Exception e) {
                log.error("Erreur lors du traitement asynchrone du follow: {}", e.getMessage(), e);
            }
        });
    }

    private void processFollowCreated(Map<String, Object> followEvent) {
        try {
            String followerId = extractString(followEvent, "followerId");
            String followedId = extractString(followEvent, "followedId");
            String followerName = extractString(followEvent, "followerName");

            if (followerId == null || followedId == null) {
                log.warn("Données manquantes dans l'événement follow: followerId={}, followedId={}", followerId, followedId);
                return;
            }

            if (!followedId.equals(followerId)) {
                // Si le nom du follower n'est pas fourni, le récupérer
                String resolvedFollowerName = followerName != null ? followerName : getUserName(followerId);

                notificationService.createFollowNotification(followedId, followerId, resolvedFollowerName);

                log.info("Notification de follow créée avec succès pour l'utilisateur suivi: {}", followedId);
            } else {
                log.warn("Tentative de follow soi-même détectée - followerId: {}", followerId);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement follow créé: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "content.created.queue")
    public void onContentCreated(Map<String, Object> contentEvent) {
        log.info("Réception d'un événement de contenu créé: {}", contentEvent);

        // Traitement asynchrone
        CompletableFuture.runAsync(() -> {
            try {
                processContentCreated(contentEvent);
            } catch (Exception e) {
                log.error("Erreur lors du traitement asynchrone du contenu: {}", e.getMessage(), e);
            }
        });
    }

    private void processContentCreated(Map<String, Object> contentEvent) {
        try {
            String creatorId = extractString(contentEvent, "userId");
            String contentId = extractString(contentEvent, "contentId");
            String contentTitle = extractString(contentEvent, "title");

            if (creatorId == null || contentId == null) {
                log.warn("Données manquantes dans l'événement content: creatorId={}, contentId={}", creatorId, contentId);
                return;
            }

            // Récupérer les followers du créateur et leur envoyer une notification
            notifyFollowersOfNewContent(creatorId, contentId, contentTitle);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement contenu créé: {}", e.getMessage(), e);
        }
    }

    @Autowired
    public EventListeners(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Méthodes utilitaires avec gestion d'erreur améliorée et circuit breaker basique
    private String getContentOwnerId(String contentId) {
        return executeWithFallback(
                () -> {
                    String url = "http://CONTENT-SERVICE/api/contents/" + contentId + "/owner";
                    String ownerId = restTemplate.getForObject(url, String.class);
                    log.debug("Owner du contenu {} récupéré: {}", contentId, ownerId);
                    return ownerId;
                },
                "Erreur lors de la récupération de l'owner du contenu " + contentId,
                null
        );
    }

    private String getUserName(String userId) {
        return executeWithFallback(
                () -> {
                    String url = "http://AUTH-SERVICE/api/users/" + userId + "/name";
                    String userName = restTemplate.getForObject(url, String.class);
                    log.debug("Nom utilisateur {} récupéré: {}", userId, userName);
                    return userName != null ? userName : "Un utilisateur";
                },
                "Erreur lors de la récupération du nom utilisateur " + userId,
                "Un utilisateur"
        );
    }

    private String getContentTitle(String contentId) {
        return executeWithFallback(
                () -> {
                    String url = "http://CONTENT-SERVICE/api/contents/" + contentId + "/title";
                    String title = restTemplate.getForObject(url, String.class);
                    log.debug("Titre du contenu {} récupéré: {}", contentId, title);
                    return title != null ? title : "un contenu";
                },
                "Erreur lors de la récupération du titre du contenu " + contentId,
                "un contenu"
        );
    }

    private void notifyFollowersOfNewContent(String creatorId, String contentId, String contentTitle) {
        executeWithFallback(
                () -> {
                    String url = "http://AUTH-SERVICE/api/users/" + creatorId + "/followers";
                    String[] followers = restTemplate.getForObject(url, String[].class);

                    if (followers != null && followers.length > 0) {
                        String creatorName = getUserName(creatorId);

                        // Traitement en parallèle des notifications
                        CompletableFuture[] futures = new CompletableFuture[followers.length];

                        for (int i = 0; i < followers.length; i++) {
                            final String followerId = followers[i];
                            futures[i] = CompletableFuture.runAsync(() -> {
                                try {
                                    notificationService.createContentNotification(
                                            followerId,
                                            creatorName,
                                            contentId,
                                            contentTitle != null ? contentTitle : "Nouveau contenu"
                                    );
                                } catch (Exception e) {
                                    log.error("Erreur lors de la création de notification pour le follower {}: {}", followerId, e.getMessage());
                                }
                            });
                        }

                        // Attendre que toutes les notifications soient envoyées
                        CompletableFuture.allOf(futures).join();

                        log.info("Notifications de nouveau contenu envoyées à {} followers", followers.length);
                    } else {
                        log.debug("Aucun follower trouvé pour le créateur: {}", creatorId);
                    }
                    return null;
                },
                "Erreur lors de la notification des followers pour le contenu " + contentId,
                null
        );
    }

    // Méthode générique pour exécuter avec fallback
    private <T> T executeWithFallback(java.util.function.Supplier<T> operation, String errorMessage, T fallbackValue) {
        try {
            return operation.get();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Ressource non trouvée: {}", e.getMessage());
            return fallbackValue;
        } catch (ResourceAccessException e) {
            log.error("Service indisponible: {}", e.getMessage());
            return fallbackValue;
        } catch (Exception e) {
            log.error("{}: {}", errorMessage, e.getMessage());
            return fallbackValue;
        }
    }

    // Méthode utilitaire pour extraire les chaînes des événements
    private String extractString(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }
}
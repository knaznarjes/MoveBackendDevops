package com.move.communitynotificationservice.service;

import com.move.communitynotificationservice.config.RabbitMQConfig;
import com.move.communitynotificationservice.model.Comment;
import com.move.communitynotificationservice.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    private final CommentRepository commentRepository;
    private final RabbitTemplate rabbitTemplate;

    // ✅ AJOUT : Injection du NotificationService
    @Autowired
    private NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, RabbitTemplate rabbitTemplate) {
        this.commentRepository = commentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public Comment addComment(Comment comment) {
        try {
            // Valider les données
            validateComment(comment);

            // Définir les timestamps
            LocalDateTime now = LocalDateTime.now();
            if (comment.getCreatedAt() == null) {
                comment.setCreatedAt(now);
            }
            comment.setUpdatedAt(now);
            comment.setDeleted(false);

            // Sauvegarder le commentaire
            Comment saved = commentRepository.save(comment);
            log.info("Commentaire sauvegardé avec l'ID: {} pour le contenu: {}", saved.getId(), saved.getContentId());

            // ✅ NOUVEAU : Créer une notification si ce n'est pas un auto-commentaire
            createCommentNotification(saved);

            // Publier l'événement de manière asynchrone
            publishCommentCreatedEvent(saved);

            return saved;
        } catch (IllegalArgumentException e) {
            log.error("Validation échouée pour le commentaire: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la création du commentaire: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de créer le commentaire", e);
        }
    }

    // ✅ NOUVELLE MÉTHODE : Créer une notification pour le commentaire
    private void createCommentNotification(Comment comment) {
        try {
            // Vérifier que contentOwnerId est défini
            if (comment.getContentOwnerId() == null || comment.getContentOwnerId().trim().isEmpty()) {
                log.warn("Impossible de créer une notification : contentOwnerId manquant pour le commentaire {}", comment.getId());
                return;
            }

            // Ne pas créer de notification si l'utilisateur commente son propre contenu
            if (comment.isSelfComment()) {
                log.debug("Pas de notification créée : l'utilisateur {} commente son propre contenu", comment.getUserId());
                return;
            }

            // Créer la notification pour le propriétaire du contenu
            String message = String.format("New comment on your content:  \"%s\"",
                    comment.getMessage().length() > 50
                            ? comment.getMessage().substring(0, 47) + "..."
                            : comment.getMessage());

            notificationService.createNotification(
                    comment.getContentOwnerId(),  // Le propriétaire du contenu recevra la notification
                    message,
                    "COMMENT",
                    comment.getContentId(),       // Source : l'ID du contenu commenté
                    null,                         // sourceName sera déterminé par le service de notification
                    buildCommentMetadata(comment)
            );

            log.info("Notification de commentaire créée pour le propriétaire du contenu: {} (commentaire: {})",
                    comment.getContentOwnerId(), comment.getId());

        } catch (Exception e) {
            log.error("Erreur lors de la création de la notification pour le commentaire {}: {}",
                    comment.getId(), e.getMessage(), e);
            // Ne pas faire échouer la création du commentaire si la notification échoue
        }
    }

    // ✅ NOUVELLE MÉTHODE : Construire les métadonnées du commentaire
    private String buildCommentMetadata(Comment comment) {
        try {
            return String.format(
                    "{\"commentId\":\"%s\",\"contentId\":\"%s\",\"userId\":\"%s\",\"message\":\"%s\"}",
                    escapeJson(comment.getId()),
                    escapeJson(comment.getContentId()),
                    escapeJson(comment.getUserId()),
                    escapeJson(comment.getMessage().length() > 100
                            ? comment.getMessage().substring(0, 100) + "..."
                            : comment.getMessage())
            );
        } catch (Exception e) {
            log.warn("Erreur lors de la construction des métadonnées du commentaire: {}", e.getMessage());
            return "{}";
        }
    }

    // ✅ NOUVELLE MÉTHODE : Échapper les caractères JSON
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByContentId(String contentId) {
        if (contentId == null || contentId.trim().isEmpty()) {
            throw new IllegalArgumentException("ContentId ne peut pas être null ou vide");
        }

        log.info("Récupération des commentaires pour le contenu: {}", contentId);

        try {
            List<Comment> comments = commentRepository.findByContentIdAndDeletedFalseOrderByCreatedAtDesc(contentId.trim());
            log.info("Trouvé {} commentaires pour le contenu: {}", comments.size(), contentId);
            return comments;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des commentaires pour le contenu {}: {}", contentId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les commentaires", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Comment> getCommentsByContentIdPaginated(String contentId, int page, int size) {
        if (contentId == null || contentId.trim().isEmpty()) {
            throw new IllegalArgumentException("ContentId ne peut pas être null ou vide");
        }

        log.info("Récupération paginée des commentaires pour le contenu: {} (page: {}, size: {})", contentId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Comment> comments = commentRepository.findByContentIdAndDeletedFalse(contentId.trim(), pageable);
            log.info("Trouvé {} commentaires (page {}/{}) pour le contenu: {}",
                    comments.getNumberOfElements(), page + 1, comments.getTotalPages(), contentId);
            return comments;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération paginée des commentaires pour le contenu {}: {}", contentId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les commentaires", e);
        }
    }

    @Transactional(readOnly = true)
    public Comment getCommentById(String commentId) {
        if (commentId == null || commentId.trim().isEmpty()) {
            throw new IllegalArgumentException("CommentId ne peut pas être null ou vide");
        }

        log.info("Récupération du commentaire: {}", commentId);

        try {
            return commentRepository.findById(commentId.trim())
                    .orElseThrow(() -> {
                        log.warn("Commentaire non trouvé avec l'ID: {}", commentId);
                        return new RuntimeException("Commentaire non trouvé avec l'ID: " + commentId);
                    });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du commentaire {}: {}", commentId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer le commentaire", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId ne peut pas être null ou vide");
        }

        log.info("Récupération des commentaires pour l'utilisateur: {}", userId);

        try {
            List<Comment> comments = commentRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId.trim());
            log.info("Trouvé {} commentaires pour l'utilisateur: {}", comments.size(), userId);
            return comments;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des commentaires pour l'utilisateur {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Impossible de récupérer les commentaires", e);
        }
    }

    @Transactional(readOnly = true)
    public long getCommentsCountByContentId(String contentId) {
        if (contentId == null || contentId.trim().isEmpty()) {
            return 0;
        }

        try {
            long count = commentRepository.countByContentIdAndDeletedFalse(contentId.trim());
            log.debug("Nombre de commentaires pour le contenu {}: {}", contentId, count);
            return count;
        } catch (Exception e) {
            log.error("Erreur lors du comptage des commentaires pour le contenu {}: {}", contentId, e.getMessage(), e);
            return 0;
        }
    }

    @Transactional
    public Comment updateComment(Comment comment) {
        try {
            if (comment.getId() == null || comment.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("ID du commentaire requis pour la mise à jour");
            }

            // Vérifier que le commentaire existe
            Comment existing = getCommentById(comment.getId());
            if (existing.isDeleted()) {
                throw new RuntimeException("Impossible de modifier un commentaire supprimé");
            }

            // Valider les nouvelles données avec getMessage()
            if (comment.getMessage() == null || comment.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("Le message du commentaire ne peut pas être vide");
            }

            // Mettre à jour avec setMessage()
            existing.setMessage(comment.getMessage().trim());
            existing.setUpdatedAt(LocalDateTime.now());

            Comment updated = commentRepository.save(existing);
            log.info("Commentaire mis à jour avec l'ID: {}", updated.getId());

            return updated;

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du commentaire: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de mettre à jour le commentaire", e);
        }
    }

    @Transactional
    public void deleteComment(String commentId) {
        if (commentId == null || commentId.trim().isEmpty()) {
            throw new IllegalArgumentException("CommentId ne peut pas être null ou vide");
        }

        log.info("Suppression du commentaire: {}", commentId);

        try {
            Comment comment = getCommentById(commentId);

            // Suppression logique (marquer comme supprimé)
            comment.markAsDeleted();
            commentRepository.save(comment);

            log.info("Commentaire marqué comme supprimé avec succès: {}", commentId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du commentaire {}: {}", commentId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer le commentaire", e);
        }
    }

    @Transactional
    public void permanentlyDeleteComment(String commentId) {
        if (commentId == null || commentId.trim().isEmpty()) {
            throw new IllegalArgumentException("CommentId ne peut pas être null ou vide");
        }

        log.info("Suppression définitive du commentaire: {}", commentId);

        try {
            if (commentRepository.existsById(commentId.trim())) {
                commentRepository.deleteById(commentId.trim());
                log.info("Commentaire supprimé définitivement avec succès: {}", commentId);
            } else {
                throw new RuntimeException("Commentaire non trouvé avec l'ID: " + commentId);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression définitive du commentaire {}: {}", commentId, e.getMessage(), e);
            throw new RuntimeException("Impossible de supprimer définitivement le commentaire", e);
        }
    }

    // Méthodes utilitaires privées
    private void validateComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Le commentaire ne peut pas être null");
        }
        if (comment.getMessage() == null || comment.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Le message du commentaire ne peut pas être vide");
        }
        if (comment.getUserId() == null || comment.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID utilisateur est requis");
        }
        if (comment.getContentId() == null || comment.getContentId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID du contenu est requis");
        }
        // ✅ NOUVEAU : Validation du contentOwnerId
        if (comment.getContentOwnerId() == null || comment.getContentOwnerId().trim().isEmpty()) {
            throw new IllegalArgumentException("L'ID du propriétaire du contenu est requis");
        }

        if (comment.getMessage().trim().length() > 2000) {
            throw new IllegalArgumentException("Le message du commentaire ne peut pas dépasser 2000 caractères");
        }
    }

    private void publishCommentCreatedEvent(Comment comment) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMMUNITY_EXCHANGE,
                    "comment.created",
                    comment
            );
            log.info("Événement comment.created publié pour le commentaire: {}", comment.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement comment.created pour le commentaire {}: {}",
                    comment.getId(), e.getMessage(), e);
            // Ne pas faire échouer la création du commentaire si la publication échoue
        }
    }

    // Méthodes de nettoyage et maintenance
    @Transactional
    public void cleanupOldDeletedComments(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        log.info("Nettoyage des commentaires supprimés antérieurs à: {}", cutoffDate);

        try {
            // Ici vous pourriez implémenter une requête pour supprimer définitivement
            // les commentaires marqués comme supprimés depuis plus de X jours
            log.info("Nettoyage des anciens commentaires supprimés terminé");
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des anciens commentaires: {}", e.getMessage(), e);
        }
    }
}
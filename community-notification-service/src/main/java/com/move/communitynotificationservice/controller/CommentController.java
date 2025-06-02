package com.move.communitynotificationservice.controller;

import com.move.communitynotificationservice.model.Comment;
import com.move.communitynotificationservice.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Extracts user ID from request attributes and ensures it's properly formatted.
     */
    private String extractSafeUserId(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        if (userId == null || userId.isEmpty()) {
            logger.error("userId is null or empty in request attributes");
            return null;
        }

        logger.debug("User ID extracted from request: {}", userId);

        // Check if userId is an email or a generated UUID
        if (userId.contains("@") || (userId.length() == 36 && userId.contains("-"))) {
            String xUserId = request.getHeader("X-User-Id");
            if (xUserId != null && !xUserId.isEmpty() && !xUserId.contains("@")) {
                logger.debug("Using ID from X-User-Id header: {}", xUserId);
                return xUserId;
            }

            // If userId is an email, convert to deterministic UUID
            if (userId.contains("@")) {
                String newId = UUID.nameUUIDFromBytes(userId.getBytes()).toString();
                logger.warn("⚠️ User ID was an email. Converting to deterministic UUID: {}", newId);
                userId = newId;
            }
        }

        return userId;
    }

    /**
     * Extracts user roles from request attributes
     */
    private String extractUserRoles(HttpServletRequest request) {
        return (String) request.getAttribute("roles");
    }

    /**
     * Checks if user has admin privileges (ADMIN or MASTERADMIN)
     */
    private boolean hasAdminRole(String roles) {
        return roles != null && (roles.contains("ADMIN") || roles.contains("MASTERADMIN"));
    }

    /**
     * Récupérer tous les commentaires d'un contenu - Accessible à tous les utilisateurs authentifiés
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/content/{contentId}")
    public ResponseEntity<?> getCommentsByContent(
            @PathVariable String contentId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {

        try {
            logger.info("Requête GET pour récupérer les commentaires du contenu: {}", contentId);

            if (contentId == null || contentId.trim().isEmpty()) {
                logger.warn("ContentId vide ou null reçu");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'ID du contenu est requis"));
            }

            // Si pagination demandée
            if (page != null && size != null) {
                if (page < 0 || size <= 0) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Paramètres de pagination invalides"));
                }

                Page<Comment> commentsPage = commentService.getCommentsByContentIdPaginated(contentId, page, size);

                Map<String, Object> response = new HashMap<>();
                response.put("comments", commentsPage.getContent());
                response.put("currentPage", commentsPage.getNumber());
                response.put("totalPages", commentsPage.getTotalPages());
                response.put("totalElements", commentsPage.getTotalElements());
                response.put("hasNext", commentsPage.hasNext());
                response.put("hasPrevious", commentsPage.hasPrevious());

                logger.info("Retour de {} commentaires (page {}/{}) pour le contenu: {}",
                        commentsPage.getNumberOfElements(), page + 1, commentsPage.getTotalPages(), contentId);

                return ResponseEntity.ok(response);
            } else {
                // Récupération simple sans pagination
                List<Comment> comments = commentService.getCommentsByContentId(contentId);

                Map<String, Object> response = new HashMap<>();
                response.put("comments", comments);
                response.put("count", comments.size());
                response.put("contentId", contentId);

                logger.info("Retour de {} commentaires pour le contenu: {}", comments.size(), contentId);
                return ResponseEntity.ok(response);
            }

        } catch (IllegalArgumentException e) {
            logger.error("Paramètres invalides pour récupération des commentaires du contenu {}: {}", contentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des commentaires pour le contenu: {}", contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des commentaires"));
        }
    }

    /**
     * Récupérer un commentaire par son ID - Accessible à tous les utilisateurs authentifiés
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{commentId}")
    public ResponseEntity<?> getComment(@PathVariable String commentId) {
        try {
            logger.info("Requête GET pour récupérer le commentaire: {}", commentId);

            if (commentId == null || commentId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'ID du commentaire est requis"));
            }

            Comment comment = commentService.getCommentById(commentId);

            logger.info("Commentaire {} récupéré avec succès", commentId);
            return ResponseEntity.ok(comment);

        } catch (RuntimeException e) {
            logger.warn("Commentaire non trouvé: {}", commentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du commentaire: {}", commentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération du commentaire"));
        }
    }

    /**
     * Récupérer le nombre de commentaires pour un contenu - Accessible à tous les utilisateurs authentifiés
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/content/{contentId}/count")
    public ResponseEntity<?> getCommentsCount(@PathVariable String contentId) {
        try {
            logger.info("Requête GET pour compter les commentaires du contenu: {}", contentId);

            if (contentId == null || contentId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'ID du contenu est requis"));
            }

            long count = commentService.getCommentsCountByContentId(contentId);

            Map<String, Object> response = new HashMap<>();
            response.put("contentId", contentId);
            response.put("count", count);

            logger.info("Nombre de commentaires pour le contenu {}: {}", contentId, count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Erreur lors du comptage des commentaires pour le contenu: {}", contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du comptage des commentaires"));
        }
    }

    /**
     * Récupérer les commentaires d'un utilisateur - Accessible à tous les utilisateurs authentifiés
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getCommentsByUser(@PathVariable String userId, HttpServletRequest request) {
        try {
            String authenticatedUserId = extractSafeUserId(request);
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Utilisateur non authentifié"));
            }

            logger.info("Requête GET pour récupérer les commentaires de l'utilisateur: {}", userId);

            List<Comment> comments = commentService.getCommentsByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments);
            response.put("count", comments.size());
            response.put("userId", userId);

            logger.info("Retour de {} commentaires pour l'utilisateur: {}", comments.size(), userId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Paramètres invalides pour récupération des commentaires de l'utilisateur {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des commentaires pour l'utilisateur: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la récupération des commentaires"));
        }
    }

    /**
     * ✅ SIMPLIFIED - Créer un nouveau commentaire
     * Let the CommentService handle notification publishing via RabbitMQ
     */
    /**
     * ✅ MODIFIED - Créer un nouveau commentaire avec contentOwnerId
     * Requires contentOwnerId in the request body
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            String userId = extractSafeUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Utilisateur non authentifié"));
            }

            // Extraire les données de la requête
            String contentId = (String) requestBody.get("contentId");
            String message = (String) requestBody.get("message");
            String contentOwnerId = (String) requestBody.get("contentOwnerId");

            // Validation des données requises
            if (contentId == null || contentId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'ID du contenu est requis"));
            }
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le message du commentaire est requis"));
            }
            if (contentOwnerId == null || contentOwnerId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'ID du propriétaire du contenu est requis"));
            }

            // Créer le commentaire avec toutes les informations
            Comment comment = new Comment(contentId.trim(), userId, message.trim(), contentOwnerId.trim());

            logger.info("Création d'un nouveau commentaire par l'utilisateur: {} pour le contenu: {} (propriétaire: {})",
                    userId, contentId, contentOwnerId);

            // Sauvegarder le commentaire (le service s'occupera de la notification)
            Comment savedComment = commentService.addComment(comment);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commentaire créé avec succès");
            response.put("comment", savedComment);

            logger.info("Commentaire créé avec succès - ID: {}", savedComment.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.error("Données invalides pour le commentaire: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la création du commentaire: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Modifier un commentaire - Accessible au propriétaire uniquement
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable String commentId,
                                           @RequestBody Map<String, String> updateData,
                                           HttpServletRequest request) {
        try {
            String userId = extractSafeUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Utilisateur non authentifié"));
            }

            Comment comment = commentService.getCommentById(commentId);

            // Seul le propriétaire du commentaire peut le modifier
            if (!comment.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Vous n'êtes pas autorisé à modifier ce commentaire"));
            }

            String newMessage = updateData.get("message");
            if (newMessage == null || newMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le message du commentaire ne peut pas être vide"));
            }

            comment.setMessage(newMessage.trim());
            Comment updatedComment = commentService.updateComment(comment);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commentaire mis à jour avec succès");
            response.put("comment", updatedComment);

            logger.info("Commentaire {} mis à jour avec succès par l'utilisateur: {}", commentId, userId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("Commentaire non trouvé ou erreur métier: {}", e.getMessage());
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la modification du commentaire: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Supprimer un commentaire - Accessible au propriétaire, aux admins et aux master admins
     */
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable String commentId, HttpServletRequest request) {
        try {
            String userId = extractSafeUserId(request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Utilisateur non authentifié"));
            }

            String userRoles = extractUserRoles(request);
            boolean isAdmin = hasAdminRole(userRoles);

            Comment comment = commentService.getCommentById(commentId);

            // Vérifier les autorisations : propriétaire OU admin/masteradmin
            if (!comment.getUserId().equals(userId) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Vous n'êtes pas autorisé à supprimer ce commentaire"));
            }

            commentService.deleteComment(commentId);

            if (isAdmin && !comment.getUserId().equals(userId)) {
                logger.info("Commentaire {} supprimé par un administrateur: {} (propriétaire: {})",
                        commentId, userId, comment.getUserId());
            } else {
                logger.info("Commentaire {} supprimé par son propriétaire: {}", commentId, userId);
            }

            return ResponseEntity.ok(Map.of("message", "Commentaire supprimé avec succès"));

        } catch (RuntimeException e) {
            logger.error("Commentaire non trouvé ou erreur métier: {}", commentId);
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du commentaire: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Endpoint de test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Community & Notification Service is running!");
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("service", "COMMUNITY-SERVICE");
        response.put("version", "1.0.0");

        logger.info("Test endpoint appelé avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * Test de connectivité à la base de données
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            // Test simple pour vérifier la connectivité
            long count = commentService.getCommentsCountByContentId("test");

            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("database", "Connected");
            response.put("service", "COMMUNITY-SERVICE");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    // Méthode utilitaire pour créer des réponses d'erreur cohérentes
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", java.time.LocalDateTime.now());
        error.put("service", "COMMUNITY-SERVICE");
        return error;
    }
}
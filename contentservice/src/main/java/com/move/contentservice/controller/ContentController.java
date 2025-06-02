package com.move.contentservice.controller;

import com.move.contentservice.dto.ContentDTO;
import com.move.contentservice.event.ContentEventPublisher;
import com.move.contentservice.mapper.ContentMapper;
import com.move.contentservice.model.Content;
import com.move.contentservice.model.ContentType;
import com.move.contentservice.repository.ContentRepository;
import com.move.contentservice.service.ContentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private static final Logger log = LoggerFactory.getLogger(ContentController.class);
    private final ContentMapper contentMapper;
    private final ContentRepository contentRepository;

    private final ContentService contentService;
    private final ContentEventPublisher contentEventPublisher;

    /**
     * Extracts user ID from request attributes and ensures it's properly formatted.
     * - Retrieves userId from request attributes or headers
     * - Validates the userId is not null or empty
     * - Handles cases where userId is an email or a generated UUID
     * - Supports fallback to X-User-Id header
     *
     * @param request The HTTP request
     * @return The properly formatted user ID or null if not available
     */
    private String extractSafeUserId(HttpServletRequest request) {
        // Get userId from request attribute (set by JwtAuthenticationFilter)
        String userId = (String) request.getAttribute("userId");

        if (userId == null || userId.isEmpty()) {
            log.error("userId is null or empty in request attributes");
            return null;
        }

        // Logging
        log.debug("User ID extracted from request: {}", userId);

        // Check if userId is an email or a generated UUID
        if (userId.contains("@") || (userId.length() == 36 && userId.contains("-"))) {
            // Debug JWT token if available
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    log.debug("JWT token found, analyzing claims");
                } catch (Exception e) {
                    log.warn("Unable to parse JWT token", e);
                }
            }

            // Check if X-User-Id header is available as alternative
            String xUserId = request.getHeader("X-User-Id");
            if (xUserId != null && !xUserId.isEmpty() && !xUserId.contains("@")) {
                log.debug("Using ID from X-User-Id header: {}", xUserId);
                return xUserId;
            }

            // If userId is an email, convert to deterministic UUID
            if (userId.contains("@")) {
                String newId = UUID.nameUUIDFromBytes(userId.getBytes()).toString();
                log.warn("⚠️ User ID was an email. Converting to deterministic UUID: {}", newId);
                userId = newId;
            }
        }

        return userId;
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PostMapping
    public ResponseEntity<ContentDTO> createContent(@RequestBody ContentDTO contentDTO, HttpServletRequest request) {
        String userId = extractSafeUserId(request);

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        contentDTO.setUserId(userId);

        // 1️⃣ Créer le contenu
        ContentDTO createdContent = contentService.createContent(contentDTO);

        // 2️⃣ Publier l'événement vers RabbitMQ (clé: content.created)
        try {
            contentEventPublisher.publishContentCreated(createdContent);
            log.info("📤 Événement 'content.created' publié pour l'ID: {}", createdContent.getId());
        } catch (Exception e) {
            log.error("❌ Échec de publication de l'événement RabbitMQ", e);
        }

        return ResponseEntity.ok(createdContent);
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @PutMapping("/{id}")
    public ResponseEntity<ContentDTO> updateContent(@PathVariable String id,
                                                    @RequestBody ContentDTO contentDTO,
                                                    HttpServletRequest request) {
        // Use method to get secure user ID
        String userId = extractSafeUserId(request);

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("Updating content ID: {} for user: {}", id, userId);

        ContentDTO existing = contentService.getContentById(id);

        // Check if the user is the owner of the content
        if (!existing.getUserId().equals(userId)) {
            log.warn("Forbidden: User {} attempting to update content owned by {}", userId, existing.getUserId());
            return ResponseEntity.status(403).body(null);
        }

        // Ensure the userId doesn't change during update
        contentDTO.setUserId(userId);

        ContentDTO updated = contentService.updateContent(id, contentDTO);

        // Publish the event after updating
        contentEventPublisher.publishContentUpdated(updated);
        log.debug("Content updated successfully");

        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<ContentDTO>> getMyContents(HttpServletRequest request) {
        // Use method to get secure user ID
        String userId = extractSafeUserId(request);

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("Fetching contents for user: {}", userId);
        List<ContentDTO> contents = contentService.getContentsByUserId(userId);
        log.debug("Found {} contents for user", contents.size());

        return ResponseEntity.ok(contents);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/all")
    public ResponseEntity<List<ContentDTO>> getAllContents() {
        return ResponseEntity.ok(contentService.getAllContents());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ContentDTO> getContentById(@PathVariable String id) {
        return ResponseEntity.ok(contentService.getContentById(id));
    }

    @PreAuthorize("hasRole('TRAVELER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable String id, HttpServletRequest request) {
        // Use method to get secure user ID
        String userId = extractSafeUserId(request);

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.debug("Attempting to delete content ID: {} by user: {}", id, userId);

        ContentDTO content = contentService.getContentById(id);

        // Check if the user is the owner of the content
        if (!content.getUserId().equals(userId)) {
            log.warn("Forbidden: User {} attempting to delete content owned by {}", userId, content.getUserId());
            return ResponseEntity.status(403).build();
        }

        // Publish the event before deleting
        contentEventPublisher.publishContentDeleted(content);

        contentService.deleteContent(id);
        log.debug("Content deleted successfully");

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/type")
    public ResponseEntity<List<ContentDTO>> getByType(@RequestParam ContentType type, HttpServletRequest request) {
        // Use method to get secure user ID
        String userId = extractSafeUserId(request);

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(contentService.getContentsByType(type, userId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/top-rated")
    public ResponseEntity<List<ContentDTO>> getTopRated(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(contentService.getTopRatedContents(PageRequest.of(page, size)));
    }

    @PreAuthorize("hasRole('MASTERADMIN')")
    @PutMapping("/{id}/block")
    public ResponseEntity<ContentDTO> blockContentAsMaster(@PathVariable String id) {
        Content blockedContent = contentService.blockContentAsMasterAdmin(id);
        ContentDTO dto = contentMapper.toDTO(blockedContent);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/block-admin")
    public ResponseEntity<?> blockContentAsAdmin(@PathVariable String id,
                                                 @RequestParam("ownerRole") String ownerRole) {
        try {
            Content blockedContent = contentService.blockContentAsAdmin(id, ownerRole);
            ContentDTO dto = contentMapper.toDTO(blockedContent);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body("⛔ Admins cannot block content from role: " + ownerRole);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Internal server error: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    @PutMapping("/{id}/publish")
    public ResponseEntity<ContentDTO> publishContent(@PathVariable String id) {
        return ResponseEntity.ok(contentService.publishContent(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ContentDTO> unpublishContent(@PathVariable String id) {
        return ResponseEntity.ok(contentService.unpublishContent(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/like")
    public ResponseEntity<ContentDTO> likeContent(@PathVariable String id) {
        return ResponseEntity.ok(contentService.likeContent(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/unlike")
    public ResponseEntity<ContentDTO> unlikeContent(@PathVariable String id) {
        return ResponseEntity.ok(contentService.unlikeContent(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/top-liked")
    public ResponseEntity<List<ContentDTO>> getTopLiked(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(contentService.getTopLikedContents(PageRequest.of(page, size)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/likes")
    public ResponseEntity<List<String>> getUserLikes(HttpServletRequest request) {
        String userId = extractSafeUserId(request);
        if (userId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(contentService.getUserLikedContentIds(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/ratings")
    public ResponseEntity<List<Map<String, Serializable>>> getUserRatings(HttpServletRequest request) {
        String userId = extractSafeUserId(request);
        if (userId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(contentService.getUserRatings(userId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ContentDTO> approveContent(@PathVariable String id) {
        Content approved = contentService.approveContent(id);
        ContentDTO dto = contentMapper.toDTO(approved);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/{contentId}/owner")
    public ResponseEntity<String> getContentOwnerId(@PathVariable String contentId) {
        Optional<Content> contentOpt = contentRepository.findById(contentId);
        if (contentOpt.isPresent()) {
            return ResponseEntity.ok(contentOpt.get().getUserId()); // ou getOwnerId()
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contenu introuvable");
        }
    }


}
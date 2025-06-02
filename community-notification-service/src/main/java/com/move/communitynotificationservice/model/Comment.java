package com.move.communitynotificationservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @Indexed
    private String contentId; // Index pour rechercher par contenu

    @Indexed
    private String userId; // Index pour rechercher par utilisateur

    // ✅ NOUVEAU : ID du propriétaire du contenu
    @Indexed
    private String contentOwnerId; // Index pour rechercher par propriétaire de contenu

    // ✅ Changé : propriété renommée "message" au lieu de "text"
    private String message;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean deleted;

    // Constructeurs
    public Comment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deleted = false;
    }

    public Comment(String contentId, String userId, String message) {
        this();
        this.contentId = contentId;
        this.userId = userId;
        this.message = message;
    }

    // ✅ NOUVEAU : Constructeur avec contentOwnerId
    public Comment(String contentId, String userId, String message, String contentOwnerId) {
        this(contentId, userId, message);
        this.contentOwnerId = contentOwnerId;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getContentId() {
        return contentId;
    }

    public String getUserId() {
        return userId;
    }

    // ✅ NOUVEAU : Getter pour contentOwnerId
    public String getContentOwnerId() {
        return contentOwnerId;
    }

    // ✅ Changé : getter renommé
    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
        this.updateTimestamp();
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.updateTimestamp();
    }

    // ✅ NOUVEAU : Setter pour contentOwnerId
    public void setContentOwnerId(String contentOwnerId) {
        this.contentOwnerId = contentOwnerId;
        this.updateTimestamp();
    }

    // ✅ Changé : setter renommé
    public void setMessage(String message) {
        this.message = message;
        this.updateTimestamp();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
        this.updateTimestamp();
    }

    // Méthodes utilitaires
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.deleted = true;
        this.updateTimestamp();
    }

    // ✅ MODIFIÉ : Validation mise à jour
    public boolean isValid() {
        return contentId != null && !contentId.trim().isEmpty() &&
                userId != null && !userId.trim().isEmpty() &&
                message != null && !message.trim().isEmpty() &&
                contentOwnerId != null && !contentOwnerId.trim().isEmpty();
    }

    // ✅ NOUVEAU : Méthode pour vérifier si c'est un auto-commentaire
    public boolean isSelfComment() {
        return userId != null && userId.equals(contentOwnerId);
    }

    // equals, hashCode et toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", contentId='" + contentId + '\'' +
                ", userId='" + userId + '\'' +
                ", contentOwnerId='" + contentOwnerId + '\'' +
                ", message='" + (message != null ? message.substring(0, Math.min(message.length(), 50)) + "..." : "null") + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deleted=" + deleted +
                '}';
    }
}
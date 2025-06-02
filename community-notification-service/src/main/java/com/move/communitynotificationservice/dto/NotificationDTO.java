package com.move.communitynotificationservice.dto;

import java.time.LocalDateTime;

public class NotificationDTO {
    private String id;
    private String userId;
    private String message;
    private String type; // COMMENT, LIKE, FOLLOW, etc.
    private String sourceId; // ID du contenu/utilisateur source
    private boolean read;
    private LocalDateTime createdAt;
    private String metadata; // JSON string pour données additionnelles

    public NotificationDTO() {}

    public NotificationDTO(String id, String userId, String message, String type,
                           String sourceId, boolean read, LocalDateTime createdAt, String metadata) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.sourceId = sourceId;
        this.read = read;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
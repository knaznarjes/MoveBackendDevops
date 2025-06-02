package com.move.communitynotificationservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection = "notifications")
@CompoundIndexes({
        @CompoundIndex(def = "{'userId': 1, 'read': 1, 'createdAt': -1}", name = "user_read_date_idx"),
        @CompoundIndex(def = "{'userId': 1, 'type': 1, 'createdAt': -1}", name = "user_type_date_idx")
})
public class Notification {

    @Id
    private String id;

    @Indexed
    private String userId; // Index pour les requêtes fréquentes par userId

    private String message;

    @Indexed
    private String type;

    private String sourceId;

    private String sourceName;

    @Indexed
    private boolean read;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
    private String metadata;
    private int priority; // Priorité de la notification (0=basse, 1=normale, 2=haute)

    public void setReceiverId(String receiverId) {
    }

    public void setIsRead(boolean b) {
    }

    public void setContentId(String contentId) {
    }

    public void setTimestamp(LocalDateTime now) {
    }

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

    // Constructeurs
    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
        this.priority = 1; // Priorité normale par défaut
    }

    public Notification(String userId, String message, String type, String sourceId) {
        this();
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.sourceId = sourceId;
    }

    public Notification(String userId, String message, String type, String sourceId, String sourceName) {
        this(userId, message, type, sourceId);
        this.sourceName = sourceName;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public int getPriority() {
        return priority;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setRead(boolean read) {
        this.read = read;
        if (read && this.readAt == null) {
            this.readAt = LocalDateTime.now();
        } else if (!read) {
            this.readAt = null;
        }
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(2, priority)); // Limiter entre 0 et 2
    }

    // Méthodes utilitaires
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    public void markAsUnread() {
        this.read = false;
        this.readAt = null;
    }

    public boolean isValid() {
        return userId != null && !userId.trim().isEmpty() &&
                message != null && !message.trim().isEmpty() &&
                type != null && !type.trim().isEmpty();
    }

    public boolean isRecent() {
        return createdAt != null && createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    public boolean isHighPriority() {
        return priority >= 2;
    }

    public long getAgeInMinutes() {
        return createdAt != null ?
                java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes() : 0;
    }

    // equals, hashCode et toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", message='" + (message != null ? message.substring(0, Math.min(message.length(), 50)) + "..." : "null") + '\'' +
                ", type='" + type + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", sourceName='" + sourceName + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                ", readAt=" + readAt +
                ", priority=" + priority +
                '}';
    }
}
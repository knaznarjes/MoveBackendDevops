package com.move.communitynotificationservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "content_meta")
public class ContentMeta {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    // Constructeurs
    public ContentMeta() {}

    public ContentMeta(String userId, String title) {
        this.userId = userId;
        this.title = title;
    }

    // Getter et Setter pour id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Getter et Setter pour userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getter et Setter pour title
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "ContentMeta{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
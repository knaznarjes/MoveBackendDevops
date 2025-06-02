
package com.move.searchrecommendationservice.model;

import java.util.List;

public class ContentItem {
    private String id;
    private String title;
    private String description;
    private List<String> tags;
    private Double budget;
    private Double rating;
    private String type;
    private String userId;

    public ContentItem() {}

    public ContentItem(String id, String title, String description, List<String> tags,
                       Double budget, Double rating, String type, String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.budget = budget;
        this.rating = rating;
        this.type = type;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

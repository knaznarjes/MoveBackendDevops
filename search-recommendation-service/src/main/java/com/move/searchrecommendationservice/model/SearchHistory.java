package com.move.searchrecommendationservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "search_history")
public class SearchHistory {
    @Id
    private String id;
    private String userId;
    private String keyword;
    private Double minBudget;
    private Double maxBudget;
    private Integer minRating;
    private String type;
    private Boolean isPublished;
    private Date timestamp;
}

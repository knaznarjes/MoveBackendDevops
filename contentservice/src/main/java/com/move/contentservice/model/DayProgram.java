package com.move.contentservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "day_programs")  // Updated from "ItineraryDay" to "day_programs"

public class DayProgram {
    @Id
    private String id;
    private Integer dayNumber;
    private String description;

    // Reference to parent Content (many-to-one)
    private String contentId;

    // One-to-many relationship with ActivityPoint
    @Builder.Default
    private List<ActivityPoint> activities = new ArrayList<>();

    // Getter qui garantit que activities n'est jamais null
    public List<ActivityPoint> getActivities() {
        if (activities == null) {
            activities = new ArrayList<>();
        }
        return activities;
    }
}
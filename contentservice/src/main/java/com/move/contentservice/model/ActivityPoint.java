package com.move.contentservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "ActivityPoint")
public class ActivityPoint {
    @Id
    private String id;
    private String name;
    private String description;
    private double cost;
    private String category;
    private String difficulty;
    private String contactInfo;

    private String dayProgramId;

    private Location location;
}

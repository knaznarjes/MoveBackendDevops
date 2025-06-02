package com.move.searchrecommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "contents") // nom exact de ta collection Mongo
public class ContentDTO {
    @Id
    private String id;
    private String title;
    private String description;
    private Double budget;
    private Integer rating;
    private String userId;
    private String type;
    private Boolean isPublished;
    private Date creationDate;
    private Date lastModified;


}
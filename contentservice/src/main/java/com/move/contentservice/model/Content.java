package com.move.contentservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "contents")
public class Content {
    @Id
    private String id;
    private String title;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Date creationDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Date lastModified;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Date startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Date endDate;

    private Double budget;
    private Boolean isPublished = false;
    private int rating;
    private int likeCount;
    private String tags;
    private int duration;
    private ContentType type;
    private String userId;
    private String coverImageId;

    // One-to-many relationship with Media
    @Builder.Default
    private List<Media> media = new ArrayList<>();

    // Many-to-many relationship with Location
    @Builder.Default
    private List<Location> locations = new ArrayList<>();

    // One-to-many relationship with DayProgram (renamed from itineraryDays)
    @Builder.Default
    private List<DayProgram> dayPrograms = new ArrayList<>();

    // Getter qui garantit que media n'est jamais null
    public List<Media> getMedia() {
        if (media == null) {
            media = new ArrayList<>();
        }
        return media;
    }

    // Getter qui garantit que locations n'est jamais null
    public List<Location> getLocations() {
        if (locations == null) {
            locations = new ArrayList<>();
        }
        return locations;
    }

    // Getter qui garantit que dayPrograms n'est jamais null (renamed from getItineraryDays)
    public List<DayProgram> getDayPrograms() {
        if (dayPrograms == null) {
            dayPrograms = new ArrayList<>();
        }
        return dayPrograms;
    }
    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }


}
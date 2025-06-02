package com.move.contentservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.move.contentservice.model.ContentType;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentDTO {
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
    private Boolean isPublished;
    private int rating;
    private int likeCount;
    private String tags;
    private int duration;
    private ContentType type;
    private String userId;
    private String coverImageId;

    @Builder.Default
    private List<MediaDTO> media = new ArrayList<>();

    @Builder.Default
    private List<LocationDTO> locations = new ArrayList<>();

    @Builder.Default
    private List<DayProgramDTO> dayPrograms = new ArrayList<>();  // Renamed from itineraryDays
}
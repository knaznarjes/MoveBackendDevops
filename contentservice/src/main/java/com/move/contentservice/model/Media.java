package com.move.contentservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Media")
public class Media {
    @Id
    private String id;
    private String title;
    private String description;
    private String fileType;
    private String fileSize;
    private String fileName;
    private String thumbnailName;  // Nom du fichier miniature
    private String mediaType;      // "COVER", "ALBUM", "VIDEO"
    private Integer displayOrder;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Date uploadDate;

    // Reference to parent Content (many-to-one)
    private String contentId;
}
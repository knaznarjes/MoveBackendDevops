package com.move.contentservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaDTO {
    private String id;
    private String title;
    private String description;
    private String fileType;
    private String fileSize;
    private String fileName;
    private String thumbnailName;  // Nom du fichier miniature
    private String mediaType;
    private Integer displayOrder;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Date uploadDate;



    private String contentId;

    // URLs pour accéder aux fichiers
    private String fileUrl;           // URL d'accès par mediaId
    private String alternativeUrl;    // URL d'accès par contentId/fileName
    private String thumbnailUrl;      // URL d'accès à la miniature
}
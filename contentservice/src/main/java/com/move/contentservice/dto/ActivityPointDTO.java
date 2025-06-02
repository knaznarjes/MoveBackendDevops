package com.move.contentservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityPointDTO {
    private String id;
    private String name;
    private String description;

    private double cost;
    private String category;
    private String difficulty;
    private String contactInfo;

    // 🟢 Renommé ici
    private String dayProgramId;

    private LocationDTO location;
}

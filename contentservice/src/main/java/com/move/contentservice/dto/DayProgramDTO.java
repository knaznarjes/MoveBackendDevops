package com.move.contentservice.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DayProgramDTO {
    private String id;
    private Integer dayNumber;
    private String description;
    private String contentId;

    @Builder.Default
    private List<ActivityPointDTO> activities = new ArrayList<>();
}
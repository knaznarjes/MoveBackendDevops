package com.move.contentservice.mapper;

import com.move.contentservice.dto.DayProgramDTO;
import com.move.contentservice.model.DayProgram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class DayProgramMapper {

    private final ActivityPointMapper activityPointMapper;

    @Autowired
    public DayProgramMapper(ActivityPointMapper activityPointMapper) {
        this.activityPointMapper = activityPointMapper;
    }

    public DayProgramDTO toDTO(DayProgram day) {
        if (day == null) {
            return null;
        }

        DayProgramDTO dto = DayProgramDTO.builder()
                .id(day.getId())
                .dayNumber(day.getDayNumber())
                .description(day.getDescription())
                .contentId(day.getContentId())
                .build();

        if (day.getActivities() != null) {
            dto.setActivities(day.getActivities().stream()
                    .map(activityPointMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public DayProgram toEntity(DayProgramDTO dto) {
        if (dto == null) {
            return null;
        }

        DayProgram day = DayProgram.builder()
                .id(dto.getId())
                .dayNumber(dto.getDayNumber())
                .description(dto.getDescription())
                .contentId(dto.getContentId())
                .build();

        if (dto.getActivities() != null) {
            day.setActivities(dto.getActivities().stream()
                    .map(activityPointMapper::toEntity)
                    .collect(Collectors.toList()));
        }

        return day;
    }
}
package com.move.contentservice.mapper;

import com.move.contentservice.dto.ActivityPointDTO;
import com.move.contentservice.model.ActivityPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityPointMapper {

    private final LocationMapper locationMapper;

    @Autowired
    public ActivityPointMapper(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    public ActivityPointDTO toDTO(ActivityPoint point) {
        if (point == null) {
            return null;
        }

        return ActivityPointDTO.builder()
                .id(point.getId())
                .name(point.getName())
                .description(point.getDescription())
                .cost(point.getCost())
                .category(point.getCategory())
                .difficulty(point.getDifficulty())
                .contactInfo(point.getContactInfo())
                .dayProgramId(point.getDayProgramId()) // 🟢 changé ici
                .location(locationMapper.toDTO(point.getLocation()))
                .build();
    }

    public ActivityPoint toEntity(ActivityPointDTO dto) {
        if (dto == null) {
            return null;
        }

        return ActivityPoint.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .cost(dto.getCost())
                .category(dto.getCategory())
                .difficulty(dto.getDifficulty())
                .contactInfo(dto.getContactInfo())
                .dayProgramId(dto.getDayProgramId()) // 🟢 changé ici
                .location(locationMapper.toEntity(dto.getLocation()))
                .build();
    }
}

package com.move.contentservice.mapper;

import com.move.contentservice.dto.ContentDTO;
import com.move.contentservice.model.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContentMapper {

    private final MediaMapper mediaMapper;
    private final LocationMapper locationMapper;
    private final DayProgramMapper dayProgramMapper;  // Renamed from itineraryDayMapper

    @Autowired
    public ContentMapper(MediaMapper mediaMapper, LocationMapper locationMapper, DayProgramMapper dayProgramMapper) {
        this.mediaMapper = mediaMapper;
        this.locationMapper = locationMapper;
        this.dayProgramMapper = dayProgramMapper;  // Renamed from itineraryDayMapper
    }

    public ContentDTO toDTO(Content content) {
        if (content == null) {
            return null;
        }

        return ContentDTO.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .creationDate(content.getCreationDate())
                .lastModified(content.getLastModified())
                .startDate(content.getStartDate())
                .endDate(content.getEndDate())
                .budget(content.getBudget())
                .isPublished(content.getIsPublished())
                .rating(content.getRating())
                .likeCount(content.getLikeCount())
                .tags(content.getTags())
                .duration(content.getDuration())
                .type(content.getType())
                .userId(content.getUserId())
                .media(content.getMedia().stream().map(mediaMapper::toDTO).collect(Collectors.toList()))
                .locations(content.getLocations().stream().map(locationMapper::toDTO).collect(Collectors.toList()))
                .dayPrograms(content.getDayPrograms().stream().map(dayProgramMapper::toDTO).collect(Collectors.toList()))  // Updated
                .coverImageId(content.getCoverImageId())
                .build();
    }

    public Content toEntity(ContentDTO dto) {
        if (dto == null) {
            return null;
        }

        Content content = Content.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .creationDate(dto.getCreationDate())
                .lastModified(dto.getLastModified())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .budget(dto.getBudget())
                .isPublished(dto.getIsPublished())
                .rating(dto.getRating())
                .likeCount(dto.getLikeCount())
                .tags(dto.getTags())
                .duration(dto.getDuration())
                .type(dto.getType())
                .userId(dto.getUserId())
                .coverImageId(dto.getCoverImageId())
                .build();

        if (dto.getMedia() != null) {
            content.setMedia(dto.getMedia().stream().map(mediaMapper::toEntity).collect(Collectors.toList()));
        }

        if (dto.getLocations() != null) {
            content.setLocations(dto.getLocations().stream().map(locationMapper::toEntity).collect(Collectors.toList()));
        }

        if (dto.getDayPrograms() != null) {  // Updated from itineraryDays
            content.setDayPrograms(dto.getDayPrograms().stream().map(dayDto -> dayProgramMapper.toEntity(dayDto)).collect(Collectors.toList()));  // Updated
        }

        return content;
    }

    public List<ContentDTO> toDTOList(List<Content> contentList) {
        return contentList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
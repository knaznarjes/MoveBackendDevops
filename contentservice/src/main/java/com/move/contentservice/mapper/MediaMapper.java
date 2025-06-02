package com.move.contentservice.mapper;

import com.move.contentservice.dto.MediaDTO;
import com.move.contentservice.model.Media;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper {

    public MediaDTO toDTO(Media media) {
        if (media == null) {
            return null;
        }

        return MediaDTO.builder()
                .id(media.getId())
                .title(media.getTitle())
                .description(media.getDescription())
                .fileType(media.getFileType())
                .fileSize(media.getFileSize())
                .uploadDate(media.getUploadDate())
                .contentId(media.getContentId())
                .mediaType(media.getMediaType())
                .displayOrder(media.getDisplayOrder())
                .fileName(media.getFileName())
                .thumbnailName(media.getThumbnailName())
                .build();
    }

    public Media toEntity(MediaDTO dto) {
        if (dto == null) {
            return null;
        }

        return Media.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .fileType(dto.getFileType())
                .fileSize(dto.getFileSize())
                .uploadDate(dto.getUploadDate())
                .contentId(dto.getContentId())
                .mediaType(dto.getMediaType())
                .displayOrder(dto.getDisplayOrder())
                .fileName(dto.getFileName())
                .thumbnailName(dto.getThumbnailName())
                .build();
    }
}
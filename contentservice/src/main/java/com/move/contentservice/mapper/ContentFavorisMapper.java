package com.move.contentservice.mapper;

import com.move.contentservice.dto.ContentFavorisDTO;
import com.move.contentservice.model.ContentFavoris;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ContentFavorisMapper {

    /**
     * Convertit une entité ContentFavoris en DTO
     */
    public ContentFavorisDTO toDTO(ContentFavoris contentFavoris) {
        if (contentFavoris == null) {
            return null;
        }

        return ContentFavorisDTO.builder()
                .id(contentFavoris.getId())
                .userId(contentFavoris.getUserId())
                .contentId(contentFavoris.getContentId())
                .dateAdded(contentFavoris.getDateAdded())
                .build();
    }

    /**
     * Convertit un DTO ContentFavoris en entité
     */
    public ContentFavoris toEntity(ContentFavorisDTO dto) {
        if (dto == null) {
            return null;
        }

        return ContentFavoris.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .contentId(dto.getContentId())
                .dateAdded(dto.getDateAdded())
                .build();
    }

    /**
     * Convertit une liste d'entités en liste de DTOs
     */
    public List<ContentFavorisDTO> toDTOList(List<ContentFavoris> contentFavorisList) {
        if (contentFavorisList == null) {
            return null;
        }

        return contentFavorisList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste de DTOs en liste d'entités
     */
    public List<ContentFavoris> toEntityList(List<ContentFavorisDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
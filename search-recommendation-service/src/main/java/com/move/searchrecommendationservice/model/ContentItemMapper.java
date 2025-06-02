
package com.move.searchrecommendationservice.model;

import com.move.searchrecommendationservice.model.ContentItem;
import com.move.searchrecommendationservice.model.ContentDTO;
import com.move.searchrecommendationservice.model.ContentIndex;

import java.util.ArrayList;
import java.util.List;

public class ContentItemMapper {
    public static ContentItem fromDTO(ContentDTO dto) {
        if (dto == null) return null;
        return new ContentItem(
                dto.getId(),
                dto.getTitle(),
                dto.getDescription(),
                null, // Pas de tags dans ContentIndex
                dto.getBudget() != null ? dto.getBudget().doubleValue() : null,
                dto.getRating() != null ? dto.getRating().doubleValue() : null,
                dto.getType(),
                dto.getUserId()
        );
    }

    public static ContentItem fromIndex(ContentIndex index) {
        if (index == null) return null;
        return new ContentItem(
                index.getId(),
                index.getTitle(),
                index.getDescription(),
                null, // Pas de tags dans ContentIndex
                index.getBudget() != null ? index.getBudget().doubleValue() : null,
                index.getRating() != null ? index.getRating().doubleValue() : null,
                index.getType(),
                index.getUserId()
        );
    }

    public static List<ContentItem> fromDTOList(List<ContentDTO> dtos) {
        List<ContentItem> result = new ArrayList<>();
        for (ContentDTO dto : dtos) {
            result.add(fromDTO(dto));
        }
        return result;
    }

    public static List<ContentItem> fromIndexList(List<ContentIndex> indexes) {
        List<ContentItem> result = new ArrayList<>();
        for (ContentIndex index : indexes) {
            result.add(fromIndex(index));
        }
        return result;
    }
}

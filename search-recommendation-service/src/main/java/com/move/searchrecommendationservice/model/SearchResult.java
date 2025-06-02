package com.move.searchrecommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;

    public int getTotalPages() {
        return pageSize > 0 ? (int) Math.ceil((double) totalElements / (double) pageSize) : 0;
    }

    public boolean hasNext() {
        return pageNumber < getTotalPages() - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }
}
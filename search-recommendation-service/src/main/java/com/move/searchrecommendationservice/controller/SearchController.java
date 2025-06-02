package com.move.searchrecommendationservice.controller;

import com.move.searchrecommendationservice.model.ContentIndex;
import com.move.searchrecommendationservice.model.SearchHistory;
import com.move.searchrecommendationservice.model.SearchResult;
import com.move.searchrecommendationservice.repository.SearchHistoryRepository;
import com.move.searchrecommendationservice.service.AdvancedSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    private final SearchHistoryRepository searchHistoryRepository;
    private final AdvancedSearchService searchService;
    private static  int suggestMaxResults = 10;

    /**
     * Public search endpoint for basic keyword search
     */
    @GetMapping("/public/keyword")
    public ResponseEntity<SearchResult<ContentIndex>> publicSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Use searchByKeyword method for simple public search
        return ResponseEntity.ok(searchService.searchByKeyword(
                keyword,
                PageRequest.of(page, size)
        ));
    }

    /**
     * Authenticated simple keyword search
     */
    @GetMapping("/keyword")
    public ResponseEntity<SearchResult<ContentIndex>> keywordSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        // Extract userId if present (JWT)
        String userId = (String) request.getAttribute("userId");

        // Log search history if userId is available
        if (userId != null) {
            searchHistoryRepository.save(SearchHistory.builder()
                    .userId(userId)
                    .keyword(keyword)
                    .timestamp(new Date())
                    .build());
        }

        return ResponseEntity.ok(searchService.searchByKeyword(
                keyword,
                PageRequest.of(page, size)
        ));
    }

    /**
     * Advanced search with multiple filters
     */
    @GetMapping("/advanced")
    public ResponseEntity<SearchResult<ContentIndex>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false, defaultValue = "rating") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        // Extract userId if present (JWT)
        String userId = (String) request.getAttribute("userId");

        if (userId != null) {
            searchHistoryRepository.save(SearchHistory.builder()
                    .userId(userId)
                    .keyword(keyword)
                    .minBudget(minBudget)
                    .maxBudget(maxBudget)
                    .minRating(minRating)
                    .type(type)
                    .isPublished(isPublished)
                    .timestamp(new Date())
                    .build());
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection.toLowerCase()), sortBy);
        return ResponseEntity.ok(searchService.advancedSearch(
                keyword, minBudget, maxBudget, minRating, type, isPublished,
                PageRequest.of(page, size, sort)
        ));
    }

    /**
     * Search for content created by the current authenticated user
     */
    @GetMapping("/my-content")
    public ResponseEntity<SearchResult<ContentIndex>> getMyContent(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = (String) request.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(searchService.findByUserId(
                userId,
                PageRequest.of(page, size)
        ));
    }

    /**
     * Search for content by specific user ID (admin only)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<SearchResult<ContentIndex>> getUserContent(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.findByUserId(
                userId,
                PageRequest.of(page, size)
        ));
    }

    /**
     * Get search suggestions based on a prefix
     */

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "10") int limit) {

        if (prefix == null || prefix.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Set the max results to the requested limit
        suggestMaxResults = Math.min(limit, 20); // Cap at 20 to prevent abuse

        List<String> suggestions = searchService.getSuggestions(prefix);
        return ResponseEntity.ok(suggestions);
    }
    /**
     * Find similar content based on a content ID
     */
    @GetMapping("/similar/{contentId}")
    public ResponseEntity<SearchResult<ContentIndex>> getSimilarContent(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.findSimilarContent(
                contentId,
                PageRequest.of(page, size)
        ));
    }

    /**
     * Get trending content based on popularity and recency
     */
    @GetMapping("/trending")
    public ResponseEntity<SearchResult<ContentIndex>> getTrendingContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.findTrendingContent(
                PageRequest.of(page, size)
        ));
    }

    /**
     * Search for content near a geographic location
     */
    @GetMapping("/location")
    public ResponseEntity<SearchResult<ContentIndex>> searchByLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double distanceKm,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.searchByLocation(
                latitude,
                longitude,
                distanceKm,
                keyword,
                isPublished,
                PageRequest.of(page, size)
        ));
    }


}
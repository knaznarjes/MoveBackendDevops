package com.move.searchrecommendationservice.repository;

import com.move.searchrecommendationservice.model.SearchHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SearchHistoryRepository extends MongoRepository<SearchHistory, String> {
    List<SearchHistory> findByUserId(String userId);
}

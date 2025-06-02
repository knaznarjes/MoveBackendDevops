package com.move.contentservice.repository;

import com.move.contentservice.model.Content;
import com.move.contentservice.model.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ContentRepository extends MongoRepository<Content, String> {
    List<Content> findByUserId(String userId);
    List<Content> findByTypeAndUserId(ContentType type, String userId);
    List<Content> findByIsPublishedTrue();
    @Query("{ 'locations.country' : ?0 }")
    List<Content> findByLocationCountry(String country);
    List<Content> findByBudgetLessThanEqual(Double maxBudget);
    List<Content> findByDurationLessThanEqual(int duration);
    List<Content> findByIsPublishedTrueOrderByRatingDesc(Pageable pageable);
    Page<Content> findByUserId(String userId, Pageable pageable); // ✅ à ajouter
    @Query("{ '$or': [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<Content> searchByKeyword(String keyword);

    Page<Content> findByIsPublishedTrueOrderByLikeCountDesc(Pageable pageable);
}

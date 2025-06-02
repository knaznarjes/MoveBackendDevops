package com.move.searchrecommendationservice.repository;

import com.move.searchrecommendationservice.model.ContentIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentIndexRepository extends ElasticsearchRepository<ContentIndex, String> {

    // Recherche de base par mot-clé dans le titre ou la description
    Page<ContentIndex> findByTitleContainingOrDescriptionContaining(String title, String description, Pageable pageable);

    // Recherche par utilisateur
    List<ContentIndex> findByUserId(String userId);

    // Exemple de requête personnalisée complexe combinant plusieurs critères
    @Query("{\"bool\": {\"must\": [{\"range\": {\"rating\": {\"gte\": ?0}}}]}}")
    Page<ContentIndex> findContentWithMinimumRating(int minRating, Pageable pageable);

    // Recherche par plage de budget
    Page<ContentIndex> findByBudgetBetween(Double minBudget, Double maxBudget, Pageable pageable);
}
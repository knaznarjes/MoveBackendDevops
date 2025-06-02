package com.move.searchrecommendationservice.repository;

import com.move.searchrecommendationservice.model.ContentDTO;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends MongoRepository<ContentDTO, String> {
}
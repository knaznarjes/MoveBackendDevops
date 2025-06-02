package com.move.communitynotificationservice.repository;

import com.move.communitynotificationservice.model.ContentMeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentMetaRepository extends MongoRepository<ContentMeta, String> {

    // Trouver par userId
    List<ContentMeta> findByUserId(String userId);

    // Trouver par title (recherche partielle)
    List<ContentMeta> findByTitleContainingIgnoreCase(String title);
}
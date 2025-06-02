package com.move.contentservice.repository;

import com.move.contentservice.model.Media;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MediaRepository extends MongoRepository<Media, String> {
    /**
     * Trouve tous les médias associés à un contenu
     */
    List<Media> findByContentId(String contentId);

    /**
     * Trouve les médias d'un type spécifique pour un contenu donné
     */
    List<Media> findByContentIdAndMediaType(String contentId, String mediaType);

    /**
     * Trouve les médias triés par ordre d'affichage
     */
    List<Media> findByContentIdOrderByDisplayOrderAsc(String contentId);

    /**
     * Trouve les médias d'un type spécifique triés par ordre d'affichage
     */
    List<Media> findByContentIdAndMediaTypeOrderByDisplayOrderAsc(String contentId, String mediaType);
}
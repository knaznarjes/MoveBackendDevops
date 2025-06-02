package com.move.contentservice.repository;

import com.move.contentservice.model.ContentFavoris;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentFavorisRepository extends MongoRepository<ContentFavoris, String> {

    /**
     * Trouve tous les favoris d'un utilisateur
     */
    List<ContentFavoris> findByUserId(String userId);

    /**
     * Trouve tous les favoris pour un contenu spécifique
     */
    List<ContentFavoris> findByContentId(String contentId);

    /**
     * Vérifie si un utilisateur a déjà ajouté un contenu en favori
     */
    Optional<ContentFavoris> findByUserIdAndContentId(String userId, String contentId);

    /**
     * Supprime un favori par userId et contentId
     */
    void deleteByUserIdAndContentId(String userId, String contentId);

    /**
     * Compte le nombre de favoris pour un contenu
     */
    long countByContentId(String contentId);

    /**
     * Vérifie si un favori existe pour un utilisateur et un contenu
     */
    boolean existsByUserIdAndContentId(String userId, String contentId);
}
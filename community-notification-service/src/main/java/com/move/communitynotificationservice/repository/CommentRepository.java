package com.move.communitynotificationservice.repository;

import com.move.communitynotificationservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    // Récupérer les commentaires par contentId (non supprimés)
    List<Comment> findByContentIdAndDeletedFalseOrderByCreatedAtDesc(String contentId);

    // Récupérer tous les commentaires par contentId (incluant supprimés)
    List<Comment> findByContentIdOrderByCreatedAtDesc(String contentId);

    // Récupérer les commentaires par utilisateur
    List<Comment> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);

    // Compter les commentaires par contenu
    long countByContentIdAndDeletedFalse(String contentId);

    // Récupérer avec pagination
    Page<Comment> findByContentIdAndDeletedFalse(String contentId, Pageable pageable);

    // Requêtes personnalisées
    @Query("{'contentId': ?0, 'deleted': false}")
    List<Comment> findActiveCommentsByContentId(String contentId);

    @Query("{'contentId': ?0, 'deleted': false, 'createdAt': {$gte: ?1}}")
    List<Comment> findRecentCommentsByContentId(String contentId, LocalDateTime since);

    // Méthode par défaut compatible avec l'ancien code
    default List<Comment> findByContentId(String contentId) {
        return findByContentIdAndDeletedFalseOrderByCreatedAtDesc(contentId);
    }
}
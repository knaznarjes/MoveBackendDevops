package com.move.communitynotificationservice.repository;

import com.move.communitynotificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Méthodes de base
    List<Notification> findByUserId(String userId);
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Tri par date décroissante
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    // Notifications non lues
    List<Notification> findByUserIdAndReadFalse(String userId);
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    // Compte des notifications non lues
    long countByUserIdAndReadFalse(String userId);

    // Notifications par type
    List<Notification> findByUserIdAndType(String userId, String type);
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);

    // Notifications dans une période
    List<Notification> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);

    // Notifications par source
    List<Notification> findByUserIdAndSourceId(String userId, String sourceId);

    // Suppression
    void deleteByUserId(String userId);
    void deleteByUserIdAndReadTrue(String userId); // Supprimer seulement les notifications lues

    // Nettoyage des anciennes notifications (plus de X jours)
    void deleteByCreatedAtBefore(LocalDateTime date);

    // Requêtes personnalisées avec @Query
    @Query("{'userId': ?0, 'read': false}")
    List<Notification> findUnreadNotificationsByUserId(String userId);

    @Query("{'userId': ?0, 'createdAt': {$gte: ?1}}")
    List<Notification> findRecentNotifications(String userId, LocalDateTime since);

    // Statistiques
    @Query(value = "{'userId': ?0}", count = true)
    long countByUserId(String userId);

    @Query(value = "{'userId': ?0, 'type': ?1}", count = true)
    long countByUserIdAndType(String userId, String type);

    List<Notification> findByUserIdAndIsReadFalse(String userId);
}
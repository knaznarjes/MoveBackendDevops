package com.move.contentservice.service;

import com.move.contentservice.dto.ContentFavorisDTO;
import com.move.contentservice.mapper.ContentFavorisMapper;
import com.move.contentservice.model.ContentFavoris;
import com.move.contentservice.repository.ContentFavorisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ContentFavorisService {

    private final ContentFavorisRepository favorisRepository;
    private final ContentFavorisMapper favorisMapper;

    @Autowired
    public ContentFavorisService(ContentFavorisRepository favorisRepository,
                                 ContentFavorisMapper favorisMapper) {
        this.favorisRepository = favorisRepository;
        this.favorisMapper = favorisMapper;
    }

    /**
     * Ajoute un contenu aux favoris d'un utilisateur
     */
    @Transactional
    public ContentFavorisDTO addToFavorites(String userId, String contentId) {
        log.info("Ajout du contenu {} aux favoris de l'utilisateur {}", contentId, userId);

        // Vérifier si le favori existe déjà
        Optional<ContentFavoris> existingFavoris = favorisRepository.findByUserIdAndContentId(userId, contentId);
        if (existingFavoris.isPresent()) {
            log.warn("Le contenu {} est déjà dans les favoris de l'utilisateur {}", contentId, userId);
            return favorisMapper.toDTO(existingFavoris.get());
        }

        // Créer un nouveau favori
        ContentFavoris favoris = ContentFavoris.builder()
                .userId(userId)
                .contentId(contentId)
                .dateAdded(new Date())
                .build();

        ContentFavoris savedFavoris = favorisRepository.save(favoris);
        log.info("Favori ajouté avec succès: {}", savedFavoris.getId());

        return favorisMapper.toDTO(savedFavoris);
    }

    /**
     * Supprime un contenu des favoris d'un utilisateur
     */
    @Transactional
    public boolean removeFromFavorites(String userId, String contentId) {
        log.info("Suppression du contenu {} des favoris de l'utilisateur {}", contentId, userId);

        Optional<ContentFavoris> favoris = favorisRepository.findByUserIdAndContentId(userId, contentId);
        if (favoris.isPresent()) {
            favorisRepository.delete(favoris.get());
            log.info("Favori supprimé avec succès");
            return true;
        } else {
            log.warn("Aucun favori trouvé pour l'utilisateur {} et le contenu {}", userId, contentId);
            return false;
        }
    }

    /**
     * Récupère tous les favoris d'un utilisateur
     */
    public List<ContentFavorisDTO> getUserFavorites(String userId) {
        log.info("Récupération des favoris de l'utilisateur {}", userId);

        List<ContentFavoris> favoris = favorisRepository.findByUserId(userId);
        return favorisMapper.toDTOList(favoris);
    }

    /**
     * Vérifie si un contenu est dans les favoris d'un utilisateur
     */
    public boolean isContentInFavorites(String userId, String contentId) {
        return favorisRepository.existsByUserIdAndContentId(userId, contentId);
    }

    /**
     * Compte le nombre de favoris pour un contenu
     */
    public long countFavoritesForContent(String contentId) {
        return favorisRepository.countByContentId(contentId);
    }

    /**
     * Récupère tous les favoris pour un contenu spécifique
     */
    public List<ContentFavorisDTO> getContentFavorites(String contentId) {
        log.info("Récupération des favoris pour le contenu {}", contentId);

        List<ContentFavoris> favoris = favorisRepository.findByContentId(contentId);
        return favorisMapper.toDTOList(favoris);
    }

    /**
     * Supprime tous les favoris d'un utilisateur
     */
    @Transactional
    public void removeAllUserFavorites(String userId) {
        log.info("Suppression de tous les favoris de l'utilisateur {}", userId);

        List<ContentFavoris> userFavorites = favorisRepository.findByUserId(userId);
        favorisRepository.deleteAll(userFavorites);

        log.info("Tous les favoris de l'utilisateur {} ont été supprimés", userId);
    }

    /**
     * Supprime tous les favoris liés à un contenu
     */
    @Transactional
    public void removeAllContentFavorites(String contentId) {
        log.info("Suppression de tous les favoris pour le contenu {}", contentId);

        List<ContentFavoris> contentFavorites = favorisRepository.findByContentId(contentId);
        favorisRepository.deleteAll(contentFavorites);

        log.info("Tous les favoris du contenu {} ont été supprimés", contentId);
    }
}
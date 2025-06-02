package com.move.contentservice.controller;

import com.move.contentservice.dto.ContentFavorisDTO;
import com.move.contentservice.service.ContentFavorisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contents/favorites")
@Tag(name = "Content Favorites", description = "API pour la gestion des favoris de contenu")
@Slf4j
public class ContentFavorisController {

    private final ContentFavorisService favorisService;

    @Autowired
    public ContentFavorisController(ContentFavorisService favorisService) {
        this.favorisService = favorisService;
    }

    @PostMapping("/users/{userId}/contents/{contentId}")
    @Operation(summary = "Ajouter un contenu aux favoris",
            description = "Ajoute un contenu spécifique aux favoris d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contenu ajouté aux favoris avec succès"),
            @ApiResponse(responseCode = "200", description = "Le contenu était déjà dans les favoris"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides")
    })
    public ResponseEntity<ContentFavorisDTO> addToFavorites(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "ID du contenu") @PathVariable String contentId) {

        log.info("Requête d'ajout aux favoris - User: {}, Content: {}", userId, contentId);

        try {
            ContentFavorisDTO favoris = favorisService.addToFavorites(userId, contentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(favoris);
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout aux favoris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/users/{userId}/contents/{contentId}")
    @Operation(summary = "Supprimer un contenu des favoris",
            description = "Retire un contenu spécifique des favoris d'un utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contenu retiré des favoris avec succès"),
            @ApiResponse(responseCode = "404", description = "Favori non trouvé")
    })
    public ResponseEntity<Void> removeFromFavorites(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "ID du contenu") @PathVariable String contentId) {

        log.info("Requête de suppression des favoris - User: {}, Content: {}", userId, contentId);

        try {
            boolean removed = favorisService.removeFromFavorites(userId, contentId);
            if (removed) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la suppression des favoris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Récupérer les favoris d'un utilisateur",
            description = "Retourne la liste de tous les favoris d'un utilisateur")
    @ApiResponse(responseCode = "200", description = "Liste des favoris récupérée avec succès")
    public ResponseEntity<List<ContentFavorisDTO>> getUserFavorites(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId) {

        log.info("Requête de récupération des favoris pour l'utilisateur: {}", userId);

        try {
            List<ContentFavorisDTO> favorites = favorisService.getUserFavorites(userId);
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des favoris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/users/{userId}/contents/{contentId}/check")
    @Operation(summary = "Vérifier si un contenu est en favori",
            description = "Vérifie si un contenu spécifique est dans les favoris d'un utilisateur")
    @ApiResponse(responseCode = "200", description = "Statut du favori retourné")
    public ResponseEntity<Boolean> isContentInFavorites(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId,
            @Parameter(description = "ID du contenu") @PathVariable String contentId) {

        try {
            boolean isFavorite = favorisService.isContentInFavorites(userId, contentId);
            return ResponseEntity.ok(isFavorite);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du favori", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/contents/{contentId}")
    @Operation(summary = "Récupérer les favoris d'un contenu",
            description = "Retourne la liste de tous les utilisateurs qui ont mis ce contenu en favori")
    @ApiResponse(responseCode = "200", description = "Liste des favoris pour le contenu récupérée")
    public ResponseEntity<List<ContentFavorisDTO>> getContentFavorites(
            @Parameter(description = "ID du contenu") @PathVariable String contentId) {

        log.info("Requête de récupération des favoris pour le contenu: {}", contentId);

        try {
            List<ContentFavorisDTO> favorites = favorisService.getContentFavorites(contentId);
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des favoris du contenu", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/contents/{contentId}/count")
    @Operation(summary = "Compter les favoris d'un contenu",
            description = "Retourne le nombre total de favoris pour un contenu")
    @ApiResponse(responseCode = "200", description = "Nombre de favoris retourné")
    public ResponseEntity<Long> countContentFavorites(
            @Parameter(description = "ID du contenu") @PathVariable String contentId) {

        try {
            long count = favorisService.countFavoritesForContent(contentId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Erreur lors du comptage des favoris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Supprimer tous les favoris d'un utilisateur",
            description = "Supprime tous les favoris d'un utilisateur spécifique")
    @ApiResponse(responseCode = "204", description = "Tous les favoris supprimés avec succès")
    public ResponseEntity<Void> removeAllUserFavorites(
            @Parameter(description = "ID de l'utilisateur") @PathVariable String userId) {

        log.info("Requête de suppression de tous les favoris de l'utilisateur: {}", userId);

        try {
            favorisService.removeAllUserFavorites(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de tous les favoris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
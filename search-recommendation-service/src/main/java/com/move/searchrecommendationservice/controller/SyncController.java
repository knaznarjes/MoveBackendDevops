package com.move.searchrecommendationservice.controller;

import com.move.searchrecommendationservice.service.SyncBatchRunner;
import com.move.searchrecommendationservice.service.SynchronizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncBatchRunner syncBatchRunner;
    private final SynchronizationService synchronizationService;

    @GetMapping
    public ResponseEntity<String> triggerSync() {
        syncBatchRunner.run(); // ou syncBatchRunner.syncAll();
        return ResponseEntity.ok("Synchronisation lancée !");
    }

    @PreAuthorize("hasRole('MASTERADMIN')")
    @PostMapping("/reset")
    public ResponseEntity<String> resetElasticsearchData() {
        try {
            // 1. Supprimer toutes les données d'Elasticsearch
            synchronizationService.deleteAllFromElasticsearch();

            // 2. Resynchroniser les données depuis MongoDB
            synchronizationService.republishAllContents();

            return ResponseEntity.ok("Index Elasticsearch réinitialisé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la réinitialisation : " + e.getMessage());
        }
    }
    @PostMapping("/{contentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    public ResponseEntity<String> syncSingleContent(@PathVariable String contentId) {
        synchronizationService.syncSingleContent(contentId);
        return ResponseEntity.ok("✅ Synchronisation du contenu " + contentId + " terminée.");
    }
    @PostMapping("/reindex-suggest")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    public ResponseEntity<String> reindexWithSuggestField() {
        try {
            synchronizationService.reindexAllWithSuggestField();
            return ResponseEntity.ok("All documents reindexed with suggest field");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error reindexing: " + e.getMessage());
        }
    }
}

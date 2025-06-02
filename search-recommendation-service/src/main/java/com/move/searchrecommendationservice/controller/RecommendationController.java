
package com.move.searchrecommendationservice.controller;

import com.move.searchrecommendationservice.model.ContentIndex;
import com.move.searchrecommendationservice.model.ContentItem;
import com.move.searchrecommendationservice.model.ContentItemMapper;
import com.move.searchrecommendationservice.repository.ContentIndexRepository;
import com.move.searchrecommendationservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendation")
public class RecommendationController {

    private final ContentIndexRepository contentIndexRepository;
    private final RecommendationService recommendationService;

    @GetMapping("/{contentId}")
    public ResponseEntity<List<ContentItem>> recommendByContentId(@PathVariable String contentId) {
        // Récupérer le contenu source
        ContentIndex content = contentIndexRepository.findById(contentId).orElse(null);
        if (content == null) return ResponseEntity.notFound().build();

        ContentItem input = ContentItemMapper.fromIndex(content);

        // Récupérer les autres contenus à comparer (en excluant celui en cours)
        List<ContentItem> candidates = StreamSupport
                .stream(contentIndexRepository.findAll().spliterator(), false)
                .filter(c -> !c.getId().equals(contentId))
                .map(ContentItemMapper::fromIndex)
                .toList();

        // Construire l'entrée NLP
        String userInput = input.getTitle() + " " + input.getDescription();

        // Appel à FastAPI
        List<ContentItem> recommendations = recommendationService.getRecommendations(userInput, candidates);

        return ResponseEntity.ok(recommendations);
    }
}

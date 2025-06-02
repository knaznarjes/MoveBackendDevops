package com.move.searchrecommendationservice.service;

import com.move.searchrecommendationservice.model.ContentDTO;
import com.move.searchrecommendationservice.model.ContentIndex;
import com.move.searchrecommendationservice.repository.ContentIndexRepository;
import com.move.searchrecommendationservice.repository.ContentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component // Permet à Spring de détecter cette classe au démarrage
public class SyncBatchRunner implements CommandLineRunner {

    private final ContentRepository contentRepository;
    private final ContentIndexRepository contentIndexRepository;

    // Injection des dépendances via constructeur
    public SyncBatchRunner(ContentRepository contentRepository,
                           ContentIndexRepository contentIndexRepository) {
        this.contentRepository = contentRepository;
        this.contentIndexRepository = contentIndexRepository;
    }

    @Override
    public void run(String... args) {
        // Récupère tous les contenus depuis MongoDB
        List<ContentDTO> contents = contentRepository.findAll();

        // Convertit chaque ContentDTO vers ContentIndex (pour Elasticsearch)
        List<ContentIndex> indexed = contents.stream().map(content -> {
            ContentIndex index = new ContentIndex();
            index.setId(content.getId());
            index.setTitle(content.getTitle());
            index.setDescription(content.getDescription());
            index.setBudget(content.getBudget());
            index.setRating(content.getRating());
            index.setUserId(content.getUserId());
            index.setType(content.getType());
            index.setIsPublished(content.getIsPublished());
            index.setCreationDate(content.getCreationDate());
            index.setLastModified(content.getLastModified());

            // Add titleSuggest field with enhanced variations for better suggestions
            if (content.getTitle() != null && !content.getTitle().isEmpty()) {
                List<String> inputs = new ArrayList<>();
                inputs.add(content.getTitle());
                inputs.add(content.getTitle().toLowerCase());

                // Add variations like replacing spaces with dashes
                if (content.getTitle().contains(" ")) {
                    inputs.add(content.getTitle().replace(" ", "-"));
                }

                // Add individual words for partial matching
                String[] words = content.getTitle().split("\\s+");
                if (words.length > 1) {
                    for (String word : words) {
                        if (word.length() > 2) { // Only words with 3+ chars
                            inputs.add(word);
                        }
                    }
                }

                index.setTitleSuggest(new Completion(inputs));
            } else {
                index.setTitleSuggest(new Completion(Collections.emptyList()));
            }

            return index;
        }).collect(Collectors.toList());

        // Enregistre tous les documents dans Elasticsearch
        contentIndexRepository.saveAll(indexed);

        System.out.println("✅ Synchronisation MongoDB -> Elasticsearch terminée avec suggestions améliorées.");
    }

    public ContentIndex getSingleContent(String id) {
        return contentRepository.findById(id)
                .map(content -> {
                    ContentIndex contentIndex = new ContentIndex();
                    contentIndex.setId(content.getId());
                    contentIndex.setTitle(content.getTitle());
                    contentIndex.setDescription(content.getDescription());
                    contentIndex.setBudget(content.getBudget());
                    contentIndex.setRating(content.getRating());
                    contentIndex.setUserId(content.getUserId());
                    contentIndex.setType(content.getType());
                    contentIndex.setIsPublished(content.getIsPublished());
                    // Add title suggest field
                    contentIndex.setTitleSuggest(new Completion(List.of(content.getTitle())));
                    return contentIndex;
                })
                .orElse(null);
    }
}
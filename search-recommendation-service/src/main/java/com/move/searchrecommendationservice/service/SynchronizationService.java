package com.move.searchrecommendationservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.move.searchrecommendationservice.model.ContentDTO;
import com.move.searchrecommendationservice.model.ContentIndex;
import com.move.searchrecommendationservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SynchronizationService {

    private final ElasticsearchClient elasticsearchClient;
    private final SyncBatchRunner syncBatchRunner;
    private final ContentRepository contentRepository;

    @Value("${elasticsearch.indices.content-index}")
    private String contentIndexName;

    public void sync(ContentDTO dto) {
        try {
            ContentIndex index = ContentIndex.builder()
                    .id(dto.getId())
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .budget(dto.getBudget())
                    .rating(dto.getRating())
                    .userId(dto.getUserId())
                    .type(dto.getType())
                    .isPublished(dto.getIsPublished())
                    .creationDate(dto.getCreationDate())
                    .lastModified(dto.getLastModified())
                    .titleSuggest(createTitleSuggestions(dto.getTitle()))  // Using our improved method
                    .build();

            elasticsearchClient.index(IndexRequest.of(i -> i
                    .index(contentIndexName)
                    .id(index.getId())
                    .document(index)
            ));
            log.info("✅ Elasticsearch document synchronized: {}", dto.getId());

        } catch (Exception e) {
            log.error("❌ Failed to sync document: {}", dto.getId(), e);
        }
    }

    public void delete(String id) {
        try {
            elasticsearchClient.delete(d -> d.index(contentIndexName).id(id));
            log.info("🗑️ Elasticsearch document deleted: {}", id);
        } catch (Exception e) {
            log.error("❌ Failed to delete document: {}", id, e);
        }
    }

    public void deleteAllFromElasticsearch() throws IOException {
        String indexName = "contentindex";

        // Vérifie si l'index existe avant de lancer la suppression
        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index(indexName))
                .value();

        if (!indexExists) {
            log.warn("🔍 Index '{}' inexistant. Suppression ignorée.", indexName);
            return;
        }

        log.info("🗑 Suppression de tous les documents de l'index '{}'", indexName);

        DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                .index(indexName)
                .query(q -> q.matchAll(m -> m))
        );

        elasticsearchClient.deleteByQuery(request);
        log.info("✅ Suppression terminée.");
    }


    public void republishAllContents() {
        syncBatchRunner.run(); // ou syncBatchRunner.syncAllFromMongoToElastic() si méthode dédiée
        log.info("Resynchronisation Mongo → Elasticsearch lancée.");
    }

    public void syncSingleContent(String contentId) {
        try {
            ContentIndex content = syncBatchRunner.getSingleContent(contentId);
            if (content == null) {
                throw new IllegalArgumentException("Content not found for ID: " + contentId);
            }

            IndexRequest<ContentIndex> request = IndexRequest.of(i -> i
                    .index(contentIndexName)
                    .id(content.getId())
                    .document(content)
            );

            elasticsearchClient.index(request);
            log.info("✅ Content {} successfully synchronized to Elasticsearch.", contentId);
        } catch (Exception e) {
            log.error("❌ Failed to sync content {}: {}", contentId, e.getMessage(), e);
            throw new RuntimeException("Synchronization failed for content: " + contentId, e);
        }
    }
    private Completion createTitleSuggestions(String title) {
        if (title == null || title.isEmpty()) {
            return new Completion(Collections.emptyList());
        }

        // Create various forms of the title for better suggestions
        List<String> inputs = new ArrayList<>();

        // Original title
        inputs.add(title);

        // Lowercased version
        inputs.add(title.toLowerCase());

        // Title with spaces replaced by dashes (common search pattern)
        if (title.contains(" ")) {
            inputs.add(title.replace(" ", "-"));
        }

        // Add individual words for partial matching
        String[] words = title.split("\\s+");
        if (words.length > 1) {
            Arrays.stream(words)
                    .filter(word -> word.length() > 2) // Only words with 3+ chars
                    .forEach(inputs::add);
        }

        return new Completion(inputs);
    }
    public void reindexAllWithSuggestField() {
        try {
            log.info("Starting reindexing process to add improved titleSuggest field to all documents");

            // Get all documents from MongoDB
            List<ContentDTO> allContent = contentRepository.findAll();
            int total = allContent.size();
            int processed = 0;

            for (ContentDTO content : allContent) {
                try {
                    // Create ContentIndex with improved titleSuggest field
                    ContentIndex index = ContentIndex.builder()
                            .id(content.getId())
                            .title(content.getTitle())
                            .description(content.getDescription())
                            .budget(content.getBudget())
                            .rating(content.getRating())
                            .userId(content.getUserId())
                            .type(content.getType())
                            .isPublished(content.getIsPublished())
                            .creationDate(content.getCreationDate())
                            .lastModified(content.getLastModified())
                            // Add the titleSuggest field with improved suggestions
                            .titleSuggest(createTitleSuggestions(content.getTitle()))
                            .build();

                    // Index document
                    elasticsearchClient.index(IndexRequest.of(i -> i
                            .index(contentIndexName)
                            .id(index.getId())
                            .document(index)
                    ));

                    processed++;
                    if (processed % 100 == 0) {
                        log.info("Reindexed {}/{} documents", processed, total);
                    }
                } catch (Exception e) {
                    log.error("Failed to reindex document {}: {}", content.getId(), e.getMessage());
                }
            }

            log.info("Reindexing completed. Successfully processed {}/{} documents", processed, total);
        } catch (Exception e) {
            log.error("Error during reindexing process", e);
            throw new RuntimeException("Failed to reindex documents with titleSuggest field", e);
        }
    }
}
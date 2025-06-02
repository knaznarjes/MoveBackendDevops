package com.move.searchrecommendationservice.service;

import com.move.searchrecommendationservice.model.ContentItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    private final RestTemplate restTemplate;

    @Value("${recommendation.fastapi-url:http://127.0.0.1:8000/recommend}")
    private String fastApiUrl;

    public RecommendationService(@Qualifier("standardRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ContentItem> getRecommendations(String userInput, List<ContentItem> contents) {
        try {
            String encodedInput = UriUtils.encode(userInput, StandardCharsets.UTF_8);
            String url = fastApiUrl + "?user_input=" + encodedInput;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<ContentItem>> entity = new HttpEntity<>(contents, headers);
            ParameterizedTypeReference<List<ContentItem>> responseType = new ParameterizedTypeReference<>() {};

            ResponseEntity<List<ContentItem>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType
            );

            List<ContentItem> recommendations = response.getBody() != null ? response.getBody() : List.of();

            // Garantir au moins 5 recommandations
            if (recommendations.size() < 5) {
                Set<String> existingIds = recommendations.stream()
                        .map(ContentItem::getId)
                        .collect(Collectors.toSet());

                List<ContentItem> remaining = contents.stream()
                        .filter(c -> !existingIds.contains(c.getId()))
                        .limit(5 - recommendations.size())
                        .collect(Collectors.toList());

                List<ContentItem> completedList = new ArrayList<>(recommendations);
                completedList.addAll(remaining);
                return completedList.stream().limit(5).toList();
            }

            return recommendations.stream().limit(5).toList();

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à FastAPI", e);
            return Collections.emptyList();
        }
    }
}

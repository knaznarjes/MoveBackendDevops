package com.move.searchrecommendationservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.JsonData;
import com.move.searchrecommendationservice.model.ContentIndex;
import com.move.searchrecommendationservice.model.SearchResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.swing.text.AbstractDocument;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedSearchService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.indices.content-index}")
    private String contentIndexName;

    @Value("${search.boost.title:2.0}")
    private Double titleBoost;

    @Value("${search.boost.description:1.0}")
    private Double descriptionBoost;

    @Value("${search.boost.rating:1.5}")
    private Double ratingBoost;

    @Value("${search.suggest.max:10}")
    private Integer suggestMaxResults;

    @Value("${search.cache.enabled:true}")
    private Boolean cacheEnabled;

    /**
     * Simple search by keyword with improved relevance scoring, synonym handling, and typo tolerance
     */
    @Cacheable(value = "keywordSearchCache", key = "#keyword + #pageable.pageNumber + #pageable.pageSize",
            condition = "@cacheEnabled")
    public SearchResult<ContentIndex> searchByKeyword(String keyword, Pageable pageable) {
        try {
            // Create function score query for better relevance
            Query baseQuery = Query.of(q -> q
                    .multiMatch(m -> m
                            .fields("title^" + titleBoost, "description^" + descriptionBoost)
                            .query(keyword)
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                            .fuzziness("AUTO")
                            .prefixLength(2)
                            .minimumShouldMatch("70%")
                    )
            );

            Query functionScoreQuery = Query.of(q -> q
                    .functionScore(fs -> fs
                            .query(baseQuery)
                            .functions(
                                    f -> f
                                            .fieldValueFactor(fvf -> fvf
                                                    .field("rating")
                                                    .factor(ratingBoost)
                                                    .modifier(co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier.Log1p)
                                                    .missing(1.0)
                                            )
                            )
                            .functions(
                                    f -> f
                                            .weight(1.2)
                                            .filter(fq -> fq
                                                    .range(r -> r
                                                            .field("lastModified")
                                                            .gte(JsonData.of("now-30d"))
                                                    )
                                            )
                            )
                            .boostMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.Multiply)
                    )
            );

            SearchRequest request = SearchRequest.of(s -> s
                    .index(contentIndexName)
                    .query(functionScoreQuery)
                    .highlight(h -> h
                            .fields("title", hf -> hf.preTags("<strong>").postTags("</strong>"))
                            .fields("description", hf -> hf.preTags("<strong>").postTags("</strong>"))
                            .numberOfFragments(3)
                            .fragmentSize(150)
                    )
                    .from(pageable.getPageNumber() * pageable.getPageSize())
                    .size(pageable.getPageSize())
                    .sort(sort -> sort.field(f -> f.field("_score").order(SortOrder.Desc)))
            );

            return executeSearch(request, pageable);
        } catch (IOException e) {
            log.error("Error during keyword search: {}", e.getMessage(), e);
            return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
        }
    }

    /**
     * Advanced search with multiple filters, aggregations, and improved relevance scoring
     */
    public SearchResult<ContentIndex> advancedSearch(
            String keyword,
            Double minBudget,
            Double maxBudget,
            Integer minRating,
            String type,
            Boolean isPublished,
            Pageable pageable) {
        try {
            // Build boolean query with filters
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // Add keyword search if provided
            if (keyword != null && !keyword.trim().isEmpty()) {
                boolQueryBuilder.must(q -> q
                        .multiMatch(mm -> mm
                                .fields("title^" + titleBoost, "description^" + descriptionBoost)
                                .query(keyword)
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                                .fuzziness("AUTO")
                                .prefixLength(2)
                                .minimumShouldMatch("70%")
                        )
                );
            } else {
                // If no keyword, use match_all for better performance
                boolQueryBuilder.must(q -> q.matchAll(ma -> ma));
            }

            // Add budget range filter if provided
            if (minBudget != null || maxBudget != null) {
                RangeQuery.Builder rangeBuilder = new RangeQuery.Builder().field("budget");

                if (minBudget != null) {
                    rangeBuilder.gte(JsonData.of(minBudget));
                }

                if (maxBudget != null) {
                    rangeBuilder.lte(JsonData.of(maxBudget));
                }

                boolQueryBuilder.filter(q -> q.range(rangeBuilder.build()));
            }

            // Add rating filter if provided
            if (minRating != null) {
                boolQueryBuilder.filter(q -> q
                        .range(r -> r
                                .field("rating")
                                .gte(JsonData.of(minRating))
                        )
                );
            }

            // Add type filter if provided
            if (type != null && !type.trim().isEmpty()) {
                boolQueryBuilder.filter(q -> q
                        .term(t -> t
                                .field("type")
                                .value(type)
                        )
                );
            }

            // Add published filter if provided
            if (isPublished != null) {
                boolQueryBuilder.filter(q -> q
                        .term(t -> t
                                .field("isPublished")
                                .value(isPublished)
                        )
                );
            }

            // Build the search request
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(contentIndexName)
                    .query(q -> q.bool(boolQueryBuilder.build()))
                    .from(pageable.getPageNumber() * pageable.getPageSize())
                    .size(pageable.getPageSize())
                    .highlight(h -> h
                            .fields("title", hf -> hf.preTags("<strong>").postTags("</strong>"))
                            .fields("description", hf -> hf.preTags("<strong>").postTags("</strong>"))
                            .numberOfFragments(3)
                            .fragmentSize(150)
                    );

            // Add aggregations for faceted search
            searchBuilder.aggregations("type_agg", a -> a
                    .terms(t -> t.field("type").size(20))
            );

            // Create range aggregation for budget
            List<AggregationRange> ranges = new ArrayList<>();
            ranges.add(new AggregationRange.Builder().key("budget").to(JsonData.of(100.0).toString()).build());
            ranges.add(new AggregationRange.Builder().key("moderate").from(JsonData.of(100.0).toString()).to(JsonData.of(500.0).toString()).build());
            ranges.add(new AggregationRange.Builder().key("premium").from(JsonData.of(500.0).toString()).to(JsonData.of(1000.0).toString()).build());
            ranges.add(new AggregationRange.Builder().key("luxury").from(JsonData.of(1000.0).toString()).build());

            searchBuilder.aggregations("budget_ranges", a -> a
                    .range(r -> r
                            .field("budget")
                            .ranges(ranges)
                    )
            );
            // Apply sorting if provided in pageable
            if (pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    searchBuilder.sort(s -> s
                            .field(f -> f
                                    .field(order.getProperty())
                                    .order(order.isAscending() ? SortOrder.Asc : SortOrder.Desc)
                            )
                    );
                });
            } else {
                // Default sort by score and then recency
                searchBuilder.sort(s -> s.field(f -> f.field("_score").order(SortOrder.Desc)));
                searchBuilder.sort(s -> s.field(f -> f.field("lastModified").order(SortOrder.Desc)));
            }

            return executeSearch(searchBuilder.build(), pageable);
        } catch (IOException e) {
            log.error("Error during advanced search: {}", e.getMessage(), e);
            return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
        }
    }

    /**
     * Find content by user ID with pagination and improved sorting
     */
    public SearchResult<ContentIndex> findByUserId(String userId, Pageable pageable) {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(contentIndexName)
                    .query(q -> q
                            .term(t -> t
                                    .field("userId")
                                    .value(userId)
                            )
                    )
                    .from(pageable.getPageNumber() * pageable.getPageSize())
                    .size(pageable.getPageSize())
                    .sort(sortBuilder -> sortBuilder.field(f -> f.field("lastModified").order(SortOrder.Desc)))
            );

            return executeSearch(request, pageable);
        } catch (IOException e) {
            log.error("Error finding content by user ID: {}", e.getMessage(), e);
            return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
        }
    }

    @Cacheable(value = "suggestionsCache", key = "#prefix", condition = "@cacheEnabled")
    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Create search request with suggester using the Elasticsearch Java API client
            SearchRequest request = SearchRequest.of(s -> s
                    .index(contentIndexName)
                    .suggest(sug -> sug
                            .suggesters("title-suggest", cs -> cs
                                    .prefix(prefix.toLowerCase())  // Use prefix at the completion level
                                    .completion(c -> c
                                            .field("titleSuggest")
                                            .skipDuplicates(true)
                                            .size(suggestMaxResults)
                                            .fuzzy(f -> f
                                                    .fuzziness("AUTO")
                                                    .minLength(3)
                                            )
                                    )
                            )
                    )
                    .size(0) // We don't need actual documents, just suggestions
            );

            // Execute request
            SearchResponse<ContentIndex> response = elasticsearchClient.search(request, ContentIndex.class);
            List<String> results = new ArrayList<>();

            // Extract suggestions with proper null checks
            if (response.suggest() != null && response.suggest().containsKey("title-suggest")) {
                response.suggest().get("title-suggest").forEach(suggestion -> {
                    if (suggestion.completion() != null && suggestion.completion().options() != null) {
                        suggestion.completion().options().forEach(option -> {
                            if (option.text() != null && !option.text().isEmpty()) {
                                // Avoid duplicates
                                if (!results.contains(option.text())) {
                                    results.add(option.text());
                                }
                            }
                        });
                    }
                });
            }

            return results;
        } catch (IOException e) {
            log.error("Error getting search suggestions: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public SearchResult<ContentIndex> findSimilarContent(String contentId, Pageable pageable) {
        try {
            // First get the original content
            SearchResponse<ContentIndex> originalResponse = elasticsearchClient.search(
                    s -> s.index(contentIndexName).query(
                            q -> q.term(t -> t.field("_id").value(contentId))
                    ),
                    ContentIndex.class
            );

            if (originalResponse.hits().total().value() == 0) {
                return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
            }

            ContentIndex original = originalResponse.hits().hits().get(0).source();

            // Use More Like This query to find similar content
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(contentIndexName)
                    .query(q -> q
                            .moreLikeThis(mlt -> mlt
                                    .fields("title", "description", "type")
                                    .like(l -> l.document(d -> d
                                            .index(contentIndexName)
                                            .id(contentId)
                                    ))
                                    .minTermFreq(1)
                                    .maxQueryTerms(12)
                                    .minimumShouldMatch("60%")
                            )
                    )
                    .from(pageable.getPageNumber() * pageable.getPageSize())
                    .size(pageable.getPageSize());

            // Execute search
            return executeSearch(requestBuilder.build(), pageable);

        } catch (IOException e) {
            log.error("Error finding similar content: {}", e.getMessage(), e);
            return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
        }
    }

    /**
     * Trending content search - find popular content based on likes and recency
     */
    public SearchResult<ContentIndex> findTrendingContent(Pageable pageable) {
        try {
            // Create function score query to boost by likes and recency
            FunctionScoreQuery.Builder functionScoreBuilder = new FunctionScoreQuery.Builder()
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.matchAll(ma -> ma))
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("isPublished")
                                                    .value(true)
                                            )
                                    )
                            )
                    );

            // Boost by likes count (popularity)
            functionScoreBuilder.functions(f -> f
                    .fieldValueFactor(fvf -> fvf
                            .field("likeCount")
                            .factor(1.5)
                            .modifier(co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier.Log1p)
                            .missing(1.0)
                    )
            );

            // Boost by recency - last 7 days gets higher boost
            functionScoreBuilder.functions(f -> f
                    .weight(2.5)
                    .filter(fq -> fq
                            .range(r -> r
                                    .field("lastModified")
                                    .gte(JsonData.of("now-7d"))
                            )
                    )
            );

            // Last 30 days gets moderate boost
            functionScoreBuilder.functions(f -> f
                    .weight(1.2)
                    .filter(fq -> fq
                            .range(r -> r
                                    .field("lastModified")
                                    .gte(JsonData.of("now-30d"))
                                    .lt(JsonData.of("now-7d"))
                            )
                    )
            );

            functionScoreBuilder.boostMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.Multiply)
                    .scoreMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode.Sum);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(contentIndexName)
                    .query(q -> q.functionScore(functionScoreBuilder.build()))
                    .from(pageable.getPageNumber() * pageable.getPageSize())
                    .size(pageable.getPageSize())
            );

            return executeSearch(request, pageable);
        } catch (IOException e) {
            log.error("Error finding trending content: {}", e.getMessage(), e);
            return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
        }
    }

    /**
     * Geolocalized search - find content near specified location
     */
    public SearchResult<ContentIndex> searchByLocation(Double lat, Double lon, Double distanceKm, String keyword,
                                                       Boolean isPublished, Pageable pageable) {
        try {
            // Build boolean query with geo filter and optional keyword
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // Add geo distance filter
            boolQueryBuilder.filter(q -> q
                    .geoDistance(g -> g
                            .field("location")
                            .distance(distanceKm + "km")
                            .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                    )
            );

            // Add keyword search if provided
            if (keyword != null && !keyword.trim().isEmpty()) {
                boolQueryBuilder.must(q -> q
                        .multiMatch(mm -> mm
                                .fields("title^" + titleBoost, "description^" + descriptionBoost)
                                .query(keyword)
                                .fuzziness("AUTO")
                        )
                );
            }

            // Add published filter if provided
            if (isPublished != null) {
                boolQueryBuilder.filter(q -> q
                        .term(t -> t
                                .field("isPublished")
                                .value(isPublished)
                        )
                );
            }

            // Create search request
            SearchRequest request = SearchRequest.of(s -> s
                    .index(contentIndexName)
                    .query(q -> q.bool(boolQueryBuilder.build()))
                    .sort(s1 -> s1
                            .geoDistance(g -> g
                                    .field("location")
                                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                                    .order(SortOrder.Asc)
                            )
                    )
                    .from(pageable.getPageNumber() * pageable.getPageSize())
                    .size(pageable.getPageSize())
            );

            return executeSearch(request, pageable);
        } catch (IOException e) {
            log.error("Error during geo search: {}", e.getMessage(), e);
            return new SearchResult<>(Collections.emptyList(), 0, 0, 0);
        }
    }

    /**
     * Execute search and transform results
     */
    private SearchResult<ContentIndex> executeSearch(SearchRequest request, Pageable pageable) throws IOException {
        SearchResponse<ContentIndex> response = elasticsearchClient.search(request, ContentIndex.class);

        List<ContentIndex> contents = response.hits().hits().stream()
                .map(hit -> {
                    ContentIndex content = hit.source();
                    // Add score information if needed for debugging/analysis
                    // content.setScore(hit.score()); - would need to add this field to ContentIndex
                    return content;
                })
                .collect(Collectors.toList());

        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

        return new SearchResult<>(
                contents,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalHits
        );
    }

    }


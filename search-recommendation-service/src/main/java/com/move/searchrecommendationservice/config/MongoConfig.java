package com.move.searchrecommendationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.move.searchrecommendationservice.repository")
public class MongoConfig {
}
package com.move.searchrecommendationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SearchRecommendationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchRecommendationServiceApplication.class, args);
    }

}

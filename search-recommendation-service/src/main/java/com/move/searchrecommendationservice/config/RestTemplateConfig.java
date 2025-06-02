package com.move.searchrecommendationservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for RestTemplate beans.
 * We need separate RestTemplate instances for service discovery (load-balanced)
 * and direct calls (non-load-balanced).
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a load-balanced RestTemplate for internal service communication
     * This is used for services registered with Eureka
     */
    @Bean
    @LoadBalanced
    @Primary
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplateBuilder().build();
    }

    /**
     * Creates a standard RestTemplate for external service communication
     * This is used for services not registered with Eureka (like FastAPI)
     */
    @Bean(name = "standardRestTemplate")
    public RestTemplate standardRestTemplate() {
        return new RestTemplateBuilder().build();
    }
}
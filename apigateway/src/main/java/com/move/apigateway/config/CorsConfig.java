package com.move.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:Authorization,Content-Type,Accept,Origin,X-Requested-With,X-Auth-User,X-Auth-Roles,x-user-id}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:Authorization,X-Auth-User,X-Auth-Roles,x-user-id}")
    private String exposedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Set allowed origins
        corsConfig.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList()));

        // Set allowed methods
        corsConfig.setAllowedMethods(Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList()));

        // Set allowed headers - INCLUT x-user-id
        corsConfig.setAllowedHeaders(Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList()));

        // Set exposed headers
        corsConfig.setExposedHeaders(Arrays.stream(exposedHeaders.split(","))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList()));

        corsConfig.setAllowCredentials(allowCredentials);
        corsConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
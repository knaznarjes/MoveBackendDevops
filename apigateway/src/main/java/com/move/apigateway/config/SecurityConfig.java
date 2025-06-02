package com.move.apigateway.config;

import com.move.apigateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RoutesConfig routesConfig;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RoutesConfig routesConfig) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.routesConfig = routesConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configuration de base
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Commencer à configurer les règles d'autorisation
        http.authorizeHttpRequests(auth -> {
            // CORS preflight d'abord - toujours permettre les requêtes OPTIONS
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

            // Endpoints publics définis dans la configuration
            if (routesConfig.getPublic() != null && !routesConfig.getPublic().isEmpty()) {
                auth.requestMatchers(routesConfig.getPublic().toArray(new String[0])).permitAll();
            }

            // Routes statiques
            if (routesConfig.getStatic() != null && !routesConfig.getStatic().isEmpty()) {
                auth.requestMatchers(routesConfig.getStatic().toArray(new String[0])).permitAll();
            }

            // Endpoints WebSocket
            auth.requestMatchers("/ws", "/ws-native").permitAll();

            // Endpoints publics hardcodés
            auth.requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh-token",
                    "/api/auth/refresh",
                    "/debug-token",
                    "/test-auth-token",
                    "/debug-auth-headers",
                    "/service-discovery-status",
                    "/actuator/**",
                    "/fallback/**",
                    "/health",
                    "/api/media/file/**",
                    "/api/media/files/**",
                    "/api/media/cover/**",
                    "/api/contents/public/**",
                    "/api/search/public/**",
                    "/api/recommendation/**"
            ).permitAll();

            // Routes admin
            if (routesConfig.getAdmin() != null && !routesConfig.getAdmin().isEmpty()) {
                auth.requestMatchers(routesConfig.getAdmin().toArray(new String[0]))
                        .hasAnyRole("ADMIN", "MASTERADMIN");
            }

            // Routes authentifiées
            if (routesConfig.getAuthenticated() != null && !routesConfig.getAuthenticated().isEmpty()) {
                auth.requestMatchers(routesConfig.getAuthenticated().toArray(new String[0]))
                        .authenticated();
            }

            // Le reste des routes nécessite une authentification
            auth.anyRequest().authenticated();
        });

        // Ajouter le filtre JWT
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "X-Auth-User", "X-Auth-Roles"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Auth-User", "X-Auth-Roles"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
package com.move.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("#{'${auth.public-endpoints}'.split(',')}")
    private List<String> publicEndpoints;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.debug("Processing request: {}", path);

        // Allow public endpoints
        if (isPublicEndpoint(path)) {
            logger.debug("Public endpoint: {}, allowing access", path);
            filterChain.doFilter(request, response);
            return;
        }

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            logger.debug("OPTIONS request: {}, allowing for CORS", path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String token = authHeader.substring(7);
        logger.debug("Token received: {}", token.substring(0, Math.min(token.length(), 20)) + "...");

        try {
            Claims claims = validateToken(token);

            if (claims == null) {
                logger.warn("Invalid JWT token for path: {}", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            String username = claims.getSubject();

            // CORRECTION: Priorité maximale au userId du claim JWT
            String userId = null;

            // Chercher directement dans le claim "userId" en premier
            if (claims.containsKey("userId")) {
                userId = claims.get("userId", String.class);
                logger.debug("userId trouvé dans le claim 'userId': {}", userId);
            }
            // Ensuite essayer avec "id" pour compatibilité
            else if (claims.containsKey("id")) {
                userId = claims.get("id", String.class);
                logger.debug("userId trouvé dans le claim 'id': {}", userId);
            }
            // Dernier recours: UUID basé sur username
            else {
                userId = UUID.nameUUIDFromBytes(username.getBytes()).toString();
                logger.warn("⚠️ Aucun ID trouvé dans le token, génération basée sur le username: {}", userId);
            }

            List<String> authorities = new ArrayList<>();
            if (claims.containsKey("authorities")) {
                authorities = claims.get("authorities", ArrayList.class);
            } else if (claims.containsKey("roles")) {
                authorities = claims.get("roles", ArrayList.class);
            }

            logger.debug("Authenticated user: {}, userId: {}, authorities: {}", username, userId, authorities);

            List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, grantedAuthorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

            // IMPORTANT: Stocker toutes les informations utilisateur utiles
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("roles", String.join(",", authorities));

            // CORRECTION: Ajouter ces informations en tant que headers pour forwarding
            response.setHeader("X-User-Id", userId);
            response.setHeader("X-Username", username);
            response.setHeader("X-User-Roles", String.join(",", authorities));

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
    private Claims validateToken(String token) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);

            if (keyBytes.length < 32) {
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            }

            Key key = Keys.hmacShaKeyFor(keyBytes);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                logger.warn("JWT token is expired");
                return null;
            }

            return claims;
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return null;
        }
    }

    private boolean isPublicEndpoint(String path) {
        return publicEndpoints.stream()
                .anyMatch(endpoint -> {
                    String trimmed = endpoint.trim();
                    if (trimmed.endsWith("/**")) {
                        String base = trimmed.substring(0, trimmed.length() - 3);
                        return path.startsWith(base);
                    }
                    return path.equals(trimmed);
                });
    }
}
package com.move.searchrecommendationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.debug("Processing request: {}", path);

        // Skip authentication for public endpoints
        if (path.startsWith("/api/search/public/") ||
                path.startsWith("/actuator/") ||
                path.equals("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow OPTIONS requests for CORS
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
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

        try {
            Claims claims = validateToken(token);

            if (claims == null) {
                logger.warn("Invalid JWT token for path: {}", path);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            String username = claims.getSubject();

            // Extract userId from claims
            String userId = null;
            if (claims.containsKey("userId")) {
                userId = claims.get("userId", String.class);
            } else if (claims.containsKey("id")) {
                userId = claims.get("id", String.class);
            } else {
                userId = UUID.nameUUIDFromBytes(username.getBytes()).toString();
                logger.warn("No ID found in token, generating based on username: {}", userId);
            }

            // Extract roles/authorities
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

            // Store user information as request attributes and headers for controllers
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("roles", String.join(",", authorities));

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
}
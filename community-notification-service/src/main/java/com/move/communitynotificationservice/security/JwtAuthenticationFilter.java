package com.move.communitynotificationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
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

        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && validateToken(token)) {
                Claims claims = parseClaims(token);

                String username = claims.getSubject();

                // CORRECTION: Priorité au userId exact du token
                String userId = null;

                // 1. Chercher d'abord directement dans le claim "userId"
                if (claims.containsKey("userId")) {
                    userId = claims.get("userId", String.class);
                    logger.debug("✅ userId récupéré depuis le claim 'userId': {}", userId);
                }
                // 2. Sinon essayer "id" (pour compatibilité)
                else if (claims.containsKey("id")) {
                    userId = claims.get("id", String.class);
                    logger.debug("✅ userId récupéré depuis le claim 'id': {}", userId);
                }
                // 3. Essayer de récupérer depuis X-User-Id header (si passé par gateway)
                else if (StringUtils.hasText(request.getHeader("X-User-Id"))) {
                    userId = request.getHeader("X-User-Id");
                    logger.debug("✅ userId récupéré depuis l'en-tête X-User-Id: {}", userId);
                }
                // 4. Générer un UUID seulement en dernier recours
                else {
                    // Ne pas générer un UUID aléatoire, mais déterministe basé sur le username
                    userId = UUID.nameUUIDFromBytes(username.getBytes()).toString();
                    logger.warn("⚠️ Aucun ID utilisateur trouvé, génération d'un UUID à partir du username: {}", userId);
                }

                // Récupérer les rôles de manière sûre
                List<String> authorities = new ArrayList<>();
                if (claims.containsKey("authorities")) {
                    authorities = ((List<?>) claims.get("authorities"))
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                } else if (claims.containsKey("roles")) {
                    authorities = ((List<?>) claims.get("roles"))
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                }

                logger.debug("Utilisateur authentifié: {}, ID: {}, Rôles: {}", username, userId, authorities);

                List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, grantedAuthorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Injecter l'ID de l'utilisateur dans la requête
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("roles", String.join(",", authorities));

                // Ajouter également les headers pour forwarding
                // Ces headers ne seront pas utilisés dans ce service mais peuvent être
                // utiles lors du forward vers d'autres services
                response.setHeader("X-User-Id", userId);
                response.setHeader("X-Username", username);
                response.setHeader("X-User-Roles", String.join(",", authorities));
            }

        } catch (ExpiredJwtException e) {
            logger.error("Token expiré : {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token expiré");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Token JWT invalide : {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalide");
            return;
        }

        filterChain.doFilter(request, response);
    }
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (StringUtils.hasText(bearer) && bearer.startsWith("Bearer "))
                ? bearer.substring(7)
                : null;
    }

    private boolean validateToken(String token) {
        try {
            parseClaims(token); // tentative de parsing, lèvera une exception si invalide
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Échec validation token JWT : {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);

        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }

        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
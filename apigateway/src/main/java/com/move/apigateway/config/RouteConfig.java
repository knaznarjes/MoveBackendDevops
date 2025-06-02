package com.move.apigateway.config;

import com.move.contentservice.dto.ContentDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RestController
public class RouteConfig {

    private static final Logger log = LoggerFactory.getLogger(RouteConfig.class);
    private final String authServiceUrl = "lb://AUTH-SERVICE";

    // AUTH routes
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> authRegister(ProxyExchange<byte[]> proxy) {
        return proxy.uri(authServiceUrl + "/api/auth/register").post();
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> authLogin(ProxyExchange<byte[]> proxy) {
        return proxy.uri(authServiceUrl + "/api/auth/login").post();
    }

    @PostMapping({"/api/auth/refresh", "/api/auth/refresh-token"})
    public ResponseEntity<?> authRefresh(ProxyExchange<byte[]> proxy,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .post();
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<?> authMe(ProxyExchange<byte[]> proxy,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying /api/auth/me request with Authorization header: {}",
                authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + "/api/auth/me")
                .get();
    }

    // ADMIN USERS routes - Ajout d'une route spécifique pour la recherche admin
    @GetMapping("/api/users/admin/search")
    public ResponseEntity<?> adminUsersSearch(ProxyExchange<byte[]> proxy,
                                              @RequestParam(required = false) String fullName,
                                              @RequestParam(required = false) String email,
                                              @RequestParam(required = false) String role,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Construction de l'URL avec les paramètres de recherche
        StringBuilder uriBuilder = new StringBuilder(authServiceUrl + "/api/users/admin/search");
        boolean hasParams = false;

        if (fullName != null && !fullName.isEmpty()) {
            uriBuilder.append(hasParams ? "&" : "?").append("fullName=").append(fullName);
            hasParams = true;
        }

        if (email != null && !email.isEmpty()) {
            uriBuilder.append(hasParams ? "&" : "?").append("email=").append(email);
            hasParams = true;
        }

        if (role != null && !role.isEmpty()) {
            uriBuilder.append(hasParams ? "&" : "?").append("role=").append(role);
        }

        log.debug("Proxying admin search to: {}", uriBuilder.toString());
        return proxy
                .header("Authorization", authorization)
                .uri(uriBuilder.toString())
                .get();
    }

    // USER routes - Fixed to properly handle HTTP methods
    @GetMapping("/api/users/admin")
    public ResponseEntity<?> usersAdminGet(ProxyExchange<byte[]> proxy,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying admin users request");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .get();
    }

    @GetMapping("/api/users/**")
    public ResponseEntity<?> usersGet(ProxyExchange<byte[]> proxy,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying GET users request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .get();
    }

    @PostMapping("/api/users/**")
    public ResponseEntity<?> usersPost(ProxyExchange<byte[]> proxy,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying POST users request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .post();
    }

    @PutMapping("/api/users/**")
    public ResponseEntity<?> usersPut(ProxyExchange<byte[]> proxy,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying PUT users request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .put();
    }

    @DeleteMapping("/api/users/**")
    public ResponseEntity<?> usersDelete(ProxyExchange<byte[]> proxy,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying DELETE users request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .delete();
    }
    @GetMapping("/api/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id,
                                         ProxyExchange<byte[]> proxy,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying GET /api/users/{} request", id);
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + "/api/users/" + id)
                .get();
    }

    // PREFERENCES routes
    @GetMapping("/api/preferences/**")
    public ResponseEntity<?> preferencesGet(ProxyExchange<byte[]> proxy,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying GET preferences request: {}", proxy.path());
        log.debug("Authorization header: {}", authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .get();
    }

    @PostMapping("/api/preferences/**")
    public ResponseEntity<?> preferencesPost(ProxyExchange<byte[]> proxy,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying POST preferences request: {}", proxy.path());
        log.debug("Authorization header: {}", authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .post();
    }

    // Enhanced debug logging for the problematic endpoints
    @PutMapping("/api/preferences/{id}")
    public ResponseEntity<?> preferencesPutById(ProxyExchange<byte[]> proxy,
                                                @PathVariable String id,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Proxying PUT preferences request for ID: {} with auth: {}",
                id, authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + "/api/preferences/" + id)
                .put();
    }

    @DeleteMapping("/api/preferences/{id}")
    public ResponseEntity<?> preferencesDeleteById(ProxyExchange<byte[]> proxy,
                                                   @PathVariable String id,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Proxying DELETE preferences request for ID: {} with auth: {}",
                id, authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + "/api/preferences/" + id)
                .delete();
    }

    // Catch-all for other preferences operations
    @PutMapping("/api/preferences/**")
    public ResponseEntity<?> preferencesPut(ProxyExchange<byte[]> proxy,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying PUT preferences request: {}", proxy.path());
        log.debug("Authorization header: {}", authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .put();
    }

    @DeleteMapping("/api/preferences/**")
    public ResponseEntity<?> preferencesDelete(ProxyExchange<byte[]> proxy,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying DELETE preferences request: {}", proxy.path());
        log.debug("Authorization header: {}", authorization != null ? "present" : "absent");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .delete();
    }

    // ACCOUNTS routes
    @GetMapping("/api/accounts/**")
    public ResponseEntity<?> accountsGet(ProxyExchange<byte[]> proxy,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying GET accounts request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .get();
    }

    @PostMapping("/api/accounts/**")
    public ResponseEntity<?> accountsPost(ProxyExchange<byte[]> proxy,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying POST accounts request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .post();
    }

    @PutMapping("/api/accounts/**")
    public ResponseEntity<?> accountsPut(ProxyExchange<byte[]> proxy,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying PUT accounts request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .put();
    }

    @DeleteMapping("/api/accounts/**")
    public ResponseEntity<?> accountsDelete(ProxyExchange<byte[]> proxy,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying DELETE accounts request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .delete();
    }

    // DEBUG routes
    @GetMapping("/api/debug-auth")
    public ResponseEntity<?> debugAuth(ProxyExchange<byte[]> proxy,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Proxying debug-auth request");
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + "/api/preferences/debug/auth")
                .get();
    }

    // ACTUATOR routes
    @RequestMapping("/actuator/**")
    public ResponseEntity<?> actuator(ProxyExchange<byte[]> proxy) {
        return proxy.uri(authServiceUrl + proxy.path()).get();
    }

    // DEBUG routes
    @RequestMapping({"/debug-token", "/service-discovery-status", "/debug-auth-headers"})
    public ResponseEntity<?> debug(ProxyExchange<byte[]> proxy,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying debug request: {}", proxy.path());
        return proxy
                .header("Authorization", authorization)
                .uri(authServiceUrl + proxy.path())
                .get();
    }

    private final String contentServiceUrl = "lb://CONTENT-SERVICE";
    private final RestTemplate restTemplate;

    public RouteConfig(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // CONTENT routes - updated according to ContentController.java
    @PostMapping("/api/contents")
    public ResponseEntity<?> createContent(
            @RequestBody ContentDTO body,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Get userId from request attributes (set by auth filter)
        String userId = (String) request.getAttribute("userId");

        // Create headers for the proxied request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        // Pass userId as a custom header
        headers.set("X-User-ID", userId != null ? userId : "");

        // Create the HttpEntity with the request body and headers
        HttpEntity<ContentDTO> requestEntity = new HttpEntity<>(body, headers);

        // Make the REST call to the content service
        ResponseEntity<Object> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents",
                HttpMethod.POST,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/user/ratings")
    public ResponseEntity<?> getUserRatings(
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-Id", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/user/ratings",
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    @GetMapping("/api/contents/user/likes")
    public ResponseEntity<?> getUserLikes(
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-Id", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/user/likes",
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/api/contents/{id}")
    public ResponseEntity<?> updateContent(
            @PathVariable String id,
            @RequestBody ContentDTO body,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Get userId from request attributes
        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId != null ? userId : "");

        HttpEntity<ContentDTO> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id,
                HttpMethod.PUT,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/me")
    public ResponseEntity<?> getMyContents(
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/me",
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/all")
    public ResponseEntity<?> getAllContents(
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/all",
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/{id}")
    public ResponseEntity<?> getContentById(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @DeleteMapping("/api/contents/{id}")
    public ResponseEntity<?> deleteContent(
            @PathVariable String id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id,
                HttpMethod.DELETE,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/type")
    public ResponseEntity<?> getContentsByType(
            @RequestParam String type,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/type?type=" + type,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/top-rated")
    public ResponseEntity<?> getTopRatedContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/top-rated?page=" + page + "&size=" + size,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("/api/contents/publish")
    public ResponseEntity<?> publishTestContent(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/publish",
                HttpMethod.POST,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/api/contents/{id}/block")
    public ResponseEntity<?> blockContentAsMasterAdmin(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id + "/block",
                HttpMethod.PUT,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/api/contents/{id}/block-admin")
    public ResponseEntity<?> blockContentAsAdmin(
            @PathVariable String id,
            @RequestParam("ownerRole") String ownerRole,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        String url = contentServiceUrl + "/api/contents/" + id + "/block-admin?ownerRole=" + ownerRole;

        ResponseEntity<?> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/api/contents/{id}/publish")
    public ResponseEntity<?> publishContent(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id + "/publish",
                HttpMethod.PUT,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/api/contents/{id}/unpublish")
    public ResponseEntity<?> unpublishContent(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id + "/unpublish",
                HttpMethod.PUT,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    @PostMapping("/api/contents/{id}/like")
    public ResponseEntity<?> likeContent(
            @PathVariable String id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id + "/like",
                HttpMethod.POST,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    @PostMapping("/api/contents/{id}/rate")
    public ResponseEntity<?> rateContent(
            @PathVariable String id,
            @RequestParam("value") int value,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Récupération sécurisée du userId injecté par JwtAuthenticationFilter
        String userId = (String) request.getAttribute("userId");

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing user ID");
        }

        // Préparer les en-têtes HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-Id", userId); // très important pour que le microservice identifie l'utilisateur

        // Créer l’entité de requête avec les headers
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        // Appel REST vers le microservice content-service avec le paramètre "value"
        String url = contentServiceUrl + "/api/contents/" + id + "/rate?value=" + value;

        ResponseEntity<?> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                Object.class
        );

        // Retourner la réponse telle quelle
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("/api/contents/{id}/unlike")
    public ResponseEntity<?> unlikeContent(
            @PathVariable String id,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId != null ? userId : "");

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id + "/unlike",
                HttpMethod.POST,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    @GetMapping("/api/contents/top-liked")
    public ResponseEntity<?> getTopLikedContents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/top-liked?page=" + page + "&size=" + size,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PutMapping("/api/contents/{id}/approve")
    public ResponseEntity<?> approveContent(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + id + "/approve",
                HttpMethod.PUT,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    // ✅ Nouvelle méthode ajoutée pour récupérer l'owner d'un content
    @GetMapping("/api/contents/{contentId}/owner")
    public ResponseEntity<String> getContentOwnerId(
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/" + contentId + "/owner",
                HttpMethod.GET,
                requestEntity,
                String.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    // CONTENT FAVORITES routes - Ajout après les routes content existantes

    @PostMapping("/api/contents/favorites/users/{userId}/contents/{contentId}")
    public ResponseEntity<?> addToFavorites(
            @PathVariable String userId,
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Vérification que l'utilisateur authentifié correspond à l'userId du path
        String authenticatedUserId = (String) request.getAttribute("userId");
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/users/" + userId + "/contents/" + contentId,
                HttpMethod.POST,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @DeleteMapping("/api/contents/favorites/users/{userId}/contents/{contentId}")
    public ResponseEntity<?> removeFromFavorites(
            @PathVariable String userId,
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Vérification que l'utilisateur authentifié correspond à l'userId du path
        String authenticatedUserId = (String) request.getAttribute("userId");
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/users/" + userId + "/contents/" + contentId,
                HttpMethod.DELETE,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/favorites/users/{userId}")
    public ResponseEntity<?> getUserFavorites(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Vérification que l'utilisateur authentifié correspond à l'userId du path
        String authenticatedUserId = (String) request.getAttribute("userId");
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/users/" + userId,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/favorites/me")
    public ResponseEntity<?> getMyFavorites(
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("Missing user ID");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/users/" + userId,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/favorites/users/{userId}/contents/{contentId}/check")
    public ResponseEntity<?> isContentInFavorites(
            @PathVariable String userId,
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Vérification que l'utilisateur authentifié correspond à l'userId du path
        String authenticatedUserId = (String) request.getAttribute("userId");
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/users/" + userId + "/contents/" + contentId + "/check",
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/favorites/contents/{contentId}")
    public ResponseEntity<?> getContentFavorites(
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/contents/" + contentId,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/contents/favorites/contents/{contentId}/count")
    public ResponseEntity<?> countContentFavorites(
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/contents/" + contentId + "/count",
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @DeleteMapping("/api/contents/favorites/users/{userId}")
    public ResponseEntity<?> removeAllUserFavorites(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Vérification que l'utilisateur authentifié correspond à l'userId du path
        String authenticatedUserId = (String) request.getAttribute("userId");
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("X-User-ID", userId);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<?> response = restTemplate.exchange(
                contentServiceUrl + "/api/contents/favorites/users/" + userId,
                HttpMethod.DELETE,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // ---- ACTIVITY POINT ----
    // Get all with pagination
    @GetMapping("/api/activity-points/paginated")
    public ResponseEntity<?> getAllActivityPointsPaginated(ProxyExchange<byte[]> proxy,
                                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/paginated")
                .get();
    }

    // Find by name
    @GetMapping("/api/activity-points/search/name")
    public ResponseEntity<?> searchActivityPointsByName(ProxyExchange<byte[]> proxy,
                                                        @RequestParam String name,
                                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/search/name?name=" + name)
                .get();
    }

    // Find by type
    @GetMapping("/api/activity-points/search/type")
    public ResponseEntity<?> searchActivityPointsByType(ProxyExchange<byte[]> proxy,
                                                        @RequestParam String type,
                                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/search/type?type=" + type)
                .get();
    }

    // Find by location
    @GetMapping("/api/activity-points/search/location")
    public ResponseEntity<?> searchActivityPointsByLocation(ProxyExchange<byte[]> proxy,
                                                            @RequestParam String location,
                                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/search/location?location=" + location)
                .get();
    }

    // Find by max cost
    @GetMapping("/api/activity-points/search/cost")
    public ResponseEntity<?> searchActivityPointsByMaxCost(ProxyExchange<byte[]> proxy,
                                                           @RequestParam Double maxCost,
                                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/search/cost?maxCost=" + maxCost)
                .get();
    }

    // Find by content ID
    @GetMapping("/api/activity-points/content/{contentId}")
    public ResponseEntity<?> getActivityPointsByContentId(ProxyExchange<byte[]> proxy,
                                                          @PathVariable String contentId,
                                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/content/" + contentId)
                .get();
    }

    // Search with multiple criteria
    @GetMapping("/api/activity-points/search")
    public ResponseEntity<?> searchActivityPointsAdvanced(ProxyExchange<byte[]> proxy,
                                                          @RequestParam(required = false) String name,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String location,
                                                          @RequestParam(required = false) Double maxCost,
                                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        StringBuilder uri = new StringBuilder(contentServiceUrl + "/api/activity-points/search?");
        if (name != null) uri.append("name=").append(name).append("&");
        if (type != null) uri.append("type=").append(type).append("&");
        if (location != null) uri.append("location=").append(location).append("&");
        if (maxCost != null) uri.append("maxCost=").append(maxCost);
        String finalUri = uri.toString().replaceAll("&$", "");

        return proxy.header("Authorization", authorization)
                .uri(finalUri)
                .get();
    }

    @GetMapping("/api/activity-points")
    public ResponseEntity<?> getAllActivityPoints(ProxyExchange<byte[]> proxy,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points")
                .get();
    }

    @GetMapping("/api/activity-points/{id}")
    public ResponseEntity<?> getActivityPointById(ProxyExchange<byte[]> proxy,
                                                  @PathVariable String id,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/" + id)
                .get();
    }

    @PostMapping("/api/activity-points")
    public ResponseEntity<?> createActivityPoint(ProxyExchange<byte[]> proxy,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points")
                .post();
    }

    @PutMapping("/api/activity-points/{id}")
    public ResponseEntity<?> updateActivityPoint(ProxyExchange<byte[]> proxy,
                                                 @PathVariable String id,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/" + id)
                .put();
    }

    @GetMapping("/api/activity-points/day-program/{dayProgramId}")
    public ResponseEntity<?> getActivityPointsByDayProgram(
            ProxyExchange<byte[]> proxy,
            @PathVariable String dayProgramId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return proxy.header("Authorization", authorization)
                .uri("lb://CONTENT-SERVICE/api/activity-points/day-program/" + dayProgramId)
                .get();
    }

    @DeleteMapping("/api/activity-points/{id}")
    public ResponseEntity<?> deleteActivityPoint(ProxyExchange<byte[]> proxy,
                                                 @PathVariable String id,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/activity-points/" + id)
                .delete();
    }

    // Specific media upload endpoints
    // Méthode helper pour l'upload multipart
    @PostMapping("/api/media/upload/cover")
    public ResponseEntity<?> uploadCoverImage(@RequestParam("file") MultipartFile file,
                                              @RequestParam("contentId") String contentId,
                                              @RequestParam("title") String title,
                                              @RequestParam("description") String description,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) throws IOException {

        String url = contentServiceUrl + "/api/media/upload/cover";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename(), file.getSize()));
        body.add("contentId", contentId);
        body.add("title", title);
        body.add("description", description);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("/api/media/upload/photo")
    public ResponseEntity<?> uploadAlbumPhoto(@RequestParam("file") MultipartFile file,
                                              @RequestParam("contentId") String contentId,
                                              @RequestParam("title") String title,
                                              @RequestParam("description") String description,
                                              @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) throws IOException {

        String url = contentServiceUrl + "/api/media/upload/photo";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename(), file.getSize()));
        body.add("contentId", contentId);
        body.add("title", title);
        body.add("description", description);
        if (displayOrder != null) body.add("displayOrder", displayOrder.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/api/media/cover/{id}")
    public ResponseEntity<?> getCoverImage(ProxyExchange<byte[]> proxy,
                                           @PathVariable String id,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/cover/" + id)
                .get();
    }
    // Ajoutez ces routes pour les photos
    @GetMapping("/api/media/photos/{contentId}")
    public ResponseEntity<?> getPhotosByContentId(ProxyExchange<byte[]> proxy,
                                                  @PathVariable String contentId,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/photos/" + contentId)
                .get();
    }

    // Ajoutez ces routes pour les vidéos
    @GetMapping("/api/media/videos/{contentId}")
    public ResponseEntity<?> getVideosByContentId(ProxyExchange<byte[]> proxy,
                                                  @PathVariable String contentId,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/videos/" + contentId)
                .get();
    }

    // Ajoutez cette route pour tous les médias d'un contenu
    @GetMapping("/api/media/content/{contentId}")
    public ResponseEntity<?> getMediaByContentId(ProxyExchange<byte[]> proxy,
                                                 @PathVariable String contentId,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/content/" + contentId)
                .get();
    }

    // Ajoutez cette route pour les médias d'un type spécifique
    @GetMapping("/api/media/content/{contentId}/type/{mediaType}")
    public ResponseEntity<?> getMediaByContentIdAndType(ProxyExchange<byte[]> proxy,
                                                        @PathVariable String contentId,
                                                        @PathVariable String mediaType,
                                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/content/" + contentId + "/type/" + mediaType)
                .get();
    }

    // Ajoutez cette route pour la mise à jour des infos média
    @PutMapping("/api/media/{mediaId}")
    public ResponseEntity<?> updateMediaInfo(ProxyExchange<byte[]> proxy,
                                             @PathVariable String mediaId,
                                             @RequestParam("title") String title,
                                             @RequestParam("description") String description,
                                             @RequestParam(value = "mediaType", required = false) String mediaType,
                                             @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {

        StringBuilder uriBuilder = new StringBuilder(contentServiceUrl + "/api/media/" + mediaId + "?");
        uriBuilder.append("title=").append(title).append("&");
        uriBuilder.append("description=").append(description);

        if (mediaType != null) {
            uriBuilder.append("&mediaType=").append(mediaType);
        }

        if (displayOrder != null) {
            uriBuilder.append("&displayOrder=").append(displayOrder);
        }

        return proxy.header("Authorization", authorization)
                .uri(uriBuilder.toString())
                .put();
    }
    @GetMapping("/api/media/file/{mediaId}")
    public ResponseEntity<?> getFileByMediaId(ProxyExchange<byte[]> proxy,
                                              @PathVariable String mediaId,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying media file request for media ID: {}", mediaId);
        return proxy
                .header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/file/" + mediaId)
                .get();
    }
    // Add these methods to RouteConfig.java

    @GetMapping("/api/media/{mediaId}")
    public ResponseEntity<?> getMediaById(ProxyExchange<byte[]> proxy,
                                          @PathVariable String mediaId,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying GET media request for ID: {}", mediaId);
        return proxy
                .header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/" + mediaId)
                .get();
    }
    @GetMapping("/media/file/{mediaId}")
    public ResponseEntity<?> getFileByMediaId(ProxyExchange<byte[]> proxy,
                                              @PathVariable String mediaId,
                                              HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        return proxy.uri("lb://CONTENT-SERVICE/api/media/file/" + mediaId)
                .header("Authorization", token)
                .get();
    }
    @GetMapping("/api/media/files/{contentId}/{fileName:.+}")
    public ResponseEntity<?> getFileByContentIdAndName(ProxyExchange<byte[]> proxy,
                                                       @PathVariable String contentId,
                                                       @PathVariable String fileName,
                                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying media file request for contentId: {} and fileName: {}", contentId, fileName);
        return proxy
                .header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/files/" + contentId + "/" + fileName)
                .get();
    }
    @DeleteMapping("/api/media/{mediaId}")
    public ResponseEntity<?> deleteMedia(ProxyExchange<byte[]> proxy,
                                         @PathVariable String mediaId,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying DELETE media request for ID: {}", mediaId);
        return proxy
                .header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/" + mediaId)
                .delete();
    }
    @GetMapping("/api/media/files/{fileName:.+}")
    public ResponseEntity<?> getMediaFile(ProxyExchange<byte[]> proxy,
                                          @PathVariable String fileName,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Proxying media file request for: {}", fileName);
        return proxy
                .header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/files/" + fileName)
                .get();
    }
    @GetMapping("/api/media/photo/{id}")
    public ResponseEntity<?> getPhoto(ProxyExchange<byte[]> proxy,
                                      @PathVariable String id,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/media/photo/" + id)
                .get();
    }

    @PostMapping("/api/media/upload/video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file,
                                         @RequestParam("contentId") String contentId,
                                         @RequestParam("title") String title,
                                         @RequestParam("description") String description,
                                         @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) throws IOException {

        String url = contentServiceUrl + "/api/media/upload/video";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename(), file.getSize()));
        body.add("contentId", contentId);
        body.add("title", title);
        body.add("description", description);
        if (displayOrder != null) body.add("displayOrder", displayOrder.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("/api/media/upload")
    public ResponseEntity<?> uploadGenericMedia(@RequestParam("file") MultipartFile file,
                                                @RequestParam("contentId") String contentId,
                                                @RequestParam("title") String title,
                                                @RequestParam("description") String description,
                                                @RequestParam(value = "mediaType", defaultValue = "ALBUM") String mediaType,
                                                @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) throws IOException {

        String url = contentServiceUrl + "/api/media/upload";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename(), file.getSize()));
        body.add("contentId", contentId);
        body.add("title", title);
        body.add("description", description);
        body.add("mediaType", mediaType);
        if (displayOrder != null) body.add("displayOrder", displayOrder.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // DAY PROGRAM routes - updated from itinerary-days to day-programs
    @PostMapping("/api/day-programs")
    public ResponseEntity<?> createDayProgram(ProxyExchange<byte[]> proxy,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/day-programs")
                .post();
    }

    @GetMapping("/api/day-programs/{id}")
    public ResponseEntity<?> getDayProgramById(ProxyExchange<byte[]> proxy,
                                               @PathVariable String id,
                                               @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/day-programs/" + id)
                .get();
    }

    @GetMapping("/api/day-programs/content/{contentId}")
    public ResponseEntity<?> getDayProgramsByContentId(ProxyExchange<byte[]> proxy,
                                                       @PathVariable String contentId,
                                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/day-programs/content/" + contentId)
                .get();
    }

    @PutMapping("/api/day-programs/{id}")
    public ResponseEntity<?> updateDayProgram(ProxyExchange<byte[]> proxy,
                                              @PathVariable String id,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/day-programs/" + id)
                .put();
    }

    @DeleteMapping("/api/day-programs/{id}")
    public ResponseEntity<?> deleteDayProgram(ProxyExchange<byte[]> proxy,
                                              @PathVariable String id,
                                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/day-programs/" + id)
                .delete();
    }
    @PostMapping("/api/locations")
    public ResponseEntity<?> createLocation(ProxyExchange<byte[]> proxy,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/locations")
                .post();
    }

    @GetMapping("/api/locations/{id}")
    public ResponseEntity<?> getLocationById(ProxyExchange<byte[]> proxy,
                                             @PathVariable String id,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/locations/" + id)
                .get();
    }

    @GetMapping("/api/locations/all")
    public ResponseEntity<?> getAllLocations(ProxyExchange<byte[]> proxy,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/locations/all")
                .get();
    }

    @PutMapping("/api/locations/{id}")
    public ResponseEntity<?> updateLocation(ProxyExchange<byte[]> proxy,
                                            @PathVariable String id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/locations/" + id)
                .put();
    }

    @DeleteMapping("/api/locations/{id}")
    public ResponseEntity<?> deleteLocation(ProxyExchange<byte[]> proxy,
                                            @PathVariable String id,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return proxy.header("Authorization", authorization)
                .uri(contentServiceUrl + "/api/locations/" + id)
                .delete();
    }
    private final String searchServiceUrl = "lb://SEARCH-SERVICE";


    // Recherche publique par mot-clé
    @GetMapping("/api/search/public/keyword")
    public ResponseEntity<?> publicKeywordSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Proxying public keyword search request with keyword: {}", keyword);
        String url = searchServiceUrl + "/api/search/public/keyword?keyword=" + keyword
                + "&page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche authentifiée par mot-clé
    @GetMapping("/api/search/keyword")
    public ResponseEntity<?> keywordSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Récupération de l'userId depuis la requête (injecté par le JwtAuthenticationFilter)
        String userId = (String) request.getAttribute("userId");

        log.debug("Proxying authenticated keyword search request with keyword: {}", keyword);
        String url = searchServiceUrl + "/api/search/keyword?keyword=" + keyword
                + "&page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche avancée avec filtres multiples
    @GetMapping("/api/search/advanced")
    public ResponseEntity<?> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minBudget,
            @RequestParam(required = false) Double maxBudget,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false, defaultValue = "rating") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Récupération de l'userId depuis la requête
        String userId = (String) request.getAttribute("userId");

        // Construction de l'URL avec tous les paramètres
        StringBuilder urlBuilder = new StringBuilder(searchServiceUrl + "/api/search/advanced?");

        if (keyword != null && !keyword.isEmpty()) {
            urlBuilder.append("keyword=").append(keyword).append("&");
        }
        if (minBudget != null) {
            urlBuilder.append("minBudget=").append(minBudget).append("&");
        }
        if (maxBudget != null) {
            urlBuilder.append("maxBudget=").append(maxBudget).append("&");
        }
        if (minRating != null) {
            urlBuilder.append("minRating=").append(minRating).append("&");
        }
        if (type != null && !type.isEmpty()) {
            urlBuilder.append("type=").append(type).append("&");
        }
        if (isPublished != null) {
            urlBuilder.append("isPublished=").append(isPublished).append("&");
        }

        urlBuilder.append("sortBy=").append(sortBy).append("&");
        urlBuilder.append("sortDirection=").append(sortDirection).append("&");
        urlBuilder.append("page=").append(page).append("&");
        urlBuilder.append("size=").append(size);

        log.debug("Proxying advanced search to: {}", urlBuilder.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche de contenus créés par l'utilisateur authentifié
    @GetMapping("/api/search/my-content")
    public ResponseEntity<?> getMyContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        // Récupération de l'userId depuis la requête
        String userId = (String) request.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.badRequest().body("User ID not found in request");
        }

        String url = searchServiceUrl + "/api/search/my-content?page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche de contenus par ID utilisateur spécifique (admin only)
    @GetMapping("/api/search/user/{userId}")
    public ResponseEntity<?> getUserContent(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {

        String url = searchServiceUrl + "/api/search/user/" + userId + "?page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Suggestions de recherche basées sur un préfixe
    @GetMapping("/api/search/suggest")
    public ResponseEntity<?> getSuggestions(
            @RequestParam String prefix,
            @RequestHeader(value = "Authorization", required = false) String token) {

        String url = searchServiceUrl + "/api/search/suggest?prefix=" + prefix;

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche de contenus similaires basée sur un ID de contenu
    @GetMapping("/api/search/similar/{contentId}")
    public ResponseEntity<?> getSimilarContent(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String token) {

        String url = searchServiceUrl + "/api/search/similar/" + contentId + "?page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche de contenus tendance basés sur la popularité et la récence
    @GetMapping("/api/search/trending")
    public ResponseEntity<?> getTrendingContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String token) {

        String url = searchServiceUrl + "/api/search/trending?page=" + page + "&size=" + size;

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    // Recherche de contenus près d'une localisation géographique
    @GetMapping("/api/search/location")
    public ResponseEntity<?> searchByLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double distanceKm,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // Construction de l'URL avec les paramètres
        StringBuilder urlBuilder = new StringBuilder(searchServiceUrl + "/api/search/location?");
        urlBuilder.append("latitude=").append(latitude).append("&");
        urlBuilder.append("longitude=").append(longitude).append("&");
        urlBuilder.append("distanceKm=").append(distanceKm).append("&");

        if (keyword != null && !keyword.isEmpty()) {
            urlBuilder.append("keyword=").append(keyword).append("&");
        }
        if (isPublished != null) {
            urlBuilder.append("isPublished=").append(isPublished).append("&");
        }

        urlBuilder.append("page=").append(page).append("&");
        urlBuilder.append("size=").append(size);

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                requestEntity,
                Object.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
    @PreAuthorize("hasRole('MASTERADMIN')")
    @PostMapping("/api/sync/reset")
    public ResponseEntity<?> proxyResetElasticsearch(
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        log.debug("Proxying reset Elasticsearch index from API Gateway to Search Service...");

        String url = searchServiceUrl + "/api/sync/reset";  // C'est correct maintenant

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying reset Elasticsearch request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " on POST request for \"" + url + "\": " + e.getResponseBodyAsString());
        }
    }
    @PostMapping("/api/sync/{contentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    public ResponseEntity<?> proxySyncSingleContent(
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token) {

        log.debug("Proxying sync single content request for contentId: {}", contentId);
        String url = searchServiceUrl + "/api/sync/" + contentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying sync single content request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Méthode pour la synchronisation complète
    @GetMapping("/api/sync")
    public ResponseEntity<?> proxyTriggerSync(
            @RequestHeader(value = "Authorization", required = false) String token) {

        log.debug("Proxying trigger sync request to Search Service...");
        String url = searchServiceUrl + "/api/sync";

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying trigger sync request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }
    @GetMapping("/api/recommendation/{contentId}")
    public ResponseEntity<?> proxyRecommendationByContentId(
            @PathVariable String contentId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        log.debug("Proxying recommendation request for contentId: {}", contentId);
        String url = searchServiceUrl + "/api/recommendation/" + contentId;

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying recommendation request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }
// ============================================================================
// ROUTES POUR LES COMMENTAIRES - VERSION CORRIGÉE
// ============================================================================
// ROUTES POUR LES COMMENTAIRES - VERSION CORRIGÉE ET HARMONISÉE
// ============================================================================

    // Récupérer les commentaires par contenu (authentifié) - avec support pagination
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/comments/content/{contentId}")
    public ResponseEntity<?> proxyGetCommentsByContent(
            @PathVariable String contentId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying get comments by content request for contentId: {} by user: {}", contentId, userId);

        // Construction de l'URL avec paramètres de pagination si présents
        StringBuilder urlBuilder = new StringBuilder(communityServiceUrl + "/api/comments/content/" + contentId);
        if (page != null && size != null) {
            urlBuilder.append("?page=").append(page).append("&size=").append(size);
        }
        String url = urlBuilder.toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying get comments by content request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Récupérer le nombre de commentaires pour un contenu (authentifié)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/comments/content/{contentId}/count")
    public ResponseEntity<?> proxyGetCommentsCount(
            @PathVariable String contentId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying get comments count request for contentId: {} by user: {}", contentId, userId);
        String url = communityServiceUrl + "/api/comments/content/" + contentId + "/count";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying get comments count request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Récupérer un commentaire par ID (authentifié)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/comments/{commentId}")
    public ResponseEntity<?> proxyGetComment(
            @PathVariable String commentId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying get comment request for commentId: {} by user: {}", commentId, userId);
        String url = communityServiceUrl + "/api/comments/" + commentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying get comment request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Récupérer les commentaires d'un utilisateur (authentifié)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/comments/user/{userId}")
    public ResponseEntity<?> proxyGetCommentsByUser(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String authenticatedUserId = (String) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying get comments by user request for userId: {} by authenticated user: {}", userId, authenticatedUserId);
        String url = communityServiceUrl + "/api/comments/user/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying get comments by user request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Créer un nouveau commentaire (authentifié)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/comments")
    public ResponseEntity<?> proxyAddComment(
            @RequestBody Object commentData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying add comment request for user: {}", userId);
        String url = communityServiceUrl + "/api/comments";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity = new HttpEntity<>(commentData, headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying add comment request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Mettre à jour un commentaire (authentifié avec rôles)
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<?> proxyUpdateComment(
            @PathVariable String commentId,
            @RequestBody Object updateData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying update comment request for commentId: {} by user: {}", commentId, userId);
        String url = communityServiceUrl + "/api/comments/" + commentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity = new HttpEntity<>(updateData, headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying update comment request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Supprimer un commentaire (authentifié avec rôles)
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<?> proxyDeleteComment(
            @PathVariable String commentId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User ID not found in request");
        }

        log.debug("Proxying delete comment request for commentId: {} by user: {}", commentId, userId);
        String url = communityServiceUrl + "/api/comments/" + commentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying delete comment request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Test endpoint pour les commentaires (public)
    @GetMapping("/api/comments/test")
    public ResponseEntity<?> proxyCommentsTest() {
        log.debug("Proxying comments test request");
        String url = communityServiceUrl + "/api/comments/test";

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying comments test request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }

    // Health check endpoint pour les commentaires (public)
    @GetMapping("/api/comments/health")
    public ResponseEntity<?> proxyCommentsHealth() {
        log.debug("Proxying comments health check request");
        String url = communityServiceUrl + "/api/comments/health";

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Error while proxying comments health check request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur proxy: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        }
    }
// ============================================================================
// ROUTES POUR LES NOTIFICATIONS
// ============================================================================

    /**
     * Récupérer les notifications d'un utilisateur (authentifié)
     */
    @GetMapping("/api/notifications/{userId}")
    public ResponseEntity<?> proxyGetNotifications(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                log.warn("Tentative d'accès aux notifications sans authentification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            // Vérification de sécurité
            if (!userId.equals(authenticatedUserId)) {
                log.warn("Tentative d'accès non autorisé aux notifications de l'utilisateur {} par {}",
                        userId, authenticatedUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé aux notifications"));
            }

            log.debug("Proxying get notifications request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId;

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la récupération des notifications pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la récupération des notifications");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy des notifications pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Récupérer les notifications avec pagination (authentifié)
     */
    @GetMapping("/api/notifications/{userId}/paginated")
    public ResponseEntity<?> proxyGetNotificationsPaginated(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            // Validation des paramètres
            if (page < 0 || size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Paramètres de pagination invalides"));
            }

            log.debug("Proxying get paginated notifications request for user: {} (page: {}, size: {})",
                    userId, page, size);

            String url = String.format("%s/api/notifications/%s/paginated?page=%d&size=%d",
                    communityServiceUrl, userId, page, size);

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la récupération paginée des notifications pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la récupération des notifications");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy des notifications paginées pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Récupérer les notifications non lues d'un utilisateur (authentifié)
     */
    @GetMapping("/api/notifications/{userId}/unread")
    public ResponseEntity<?> proxyGetUnreadNotifications(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Proxying get unread notifications request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId + "/unread";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la récupération des notifications non lues pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la récupération des notifications non lues");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy des notifications non lues pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Récupérer le nombre de notifications non lues (authentifié)
     */
    @GetMapping("/api/notifications/{userId}/count")
    public ResponseEntity<?> proxyGetUnreadCount(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Proxying get unread count request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId + "/count";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la récupération du compte de notifications pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la récupération du compte");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy du compte de notifications pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Récupérer une notification spécifique par ID (authentifié)
     */
    @GetMapping("/api/notifications/notification/{notificationId}")
    public ResponseEntity<?> proxyGetNotificationById(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            log.debug("Proxying get notification by ID request for notification: {} by user: {}", notificationId, userId);
            String url = communityServiceUrl + "/api/notifications/notification/" + notificationId;

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la récupération de la notification {}: {}", notificationId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la récupération de la notification");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de la notification {}: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Récupérer les statistiques de notifications (authentifié)
     */
    @GetMapping("/api/notifications/{userId}/stats")
    public ResponseEntity<?> proxyGetNotificationStats(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Proxying get notification stats request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId + "/stats";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la récupération des statistiques pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la récupération des statistiques");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy des statistiques pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Marquer une notification comme lue (authentifié)
     */
    @PutMapping("/api/notifications/{notificationId}/read")
    public ResponseEntity<?> proxyMarkAsRead(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            log.debug("Proxying mark as read request for notification: {} by user: {}", notificationId, userId);
            String url = communityServiceUrl + "/api/notifications/" + notificationId + "/read";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.PUT, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors du marquage comme lu de la notification {}: {}", notificationId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la mise à jour");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de marquage comme lu pour {}: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Marquer toutes les notifications comme lues (authentifié)
     */
    @PutMapping("/api/notifications/{userId}/read-all")
    public ResponseEntity<?> proxyMarkAllAsRead(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Proxying mark all as read request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId + "/read-all";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.PUT, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors du marquage de toutes les notifications comme lues pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la mise à jour");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de marquage de toutes comme lues pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Supprimer une notification (authentifié)
     */
    @DeleteMapping("/api/notifications/{notificationId}")
    public ResponseEntity<?> proxyDeleteNotification(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            log.debug("Proxying delete notification request for notification: {} by user: {}", notificationId, userId);
            String url = communityServiceUrl + "/api/notifications/" + notificationId;

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la suppression de la notification {}: {}", notificationId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la suppression");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de suppression pour {}: {}", notificationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Supprimer toutes les notifications d'un utilisateur (authentifié)
     */
    @DeleteMapping("/api/notifications/{userId}/all")
    public ResponseEntity<?> proxyDeleteAllNotifications(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Proxying delete all notifications request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId + "/all";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la suppression de toutes les notifications pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors de la suppression");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de suppression toutes notifications pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Nettoyer les notifications lues d'un utilisateur (authentifié)
     */
    @DeleteMapping("/api/notifications/{userId}/cleanup-read")
    public ResponseEntity<?> proxyCleanupReadNotifications(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Proxying cleanup read notifications request for user: {}", userId);
            String url = communityServiceUrl + "/api/notifications/" + userId + "/cleanup-read";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors du nettoyage des notifications lues pour {}: {}", userId, e.getMessage());
            return handleProxyError(e, "Erreur lors du nettoyage");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de nettoyage pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    // =============== MÉTHODES UTILITAIRES POUR LES NOTIFICATIONS ===============

    /**
     * Crée les headers pour les requêtes proxy
     */
    private HttpHeaders createProxyHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Gère les erreurs de proxy de manière centralisée
     */
    private ResponseEntity<?> handleProxyError(HttpStatusCodeException e, String defaultMessage) {
        String errorMessage = defaultMessage;
        try {
            // Tenter d'extraire le message d'erreur du service distant
            if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty()) {
                errorMessage = defaultMessage + ": " + e.getResponseBodyAsString();
            }
        } catch (Exception ex) {
            log.debug("Impossible d'extraire le message d'erreur détaillé: {}", ex.getMessage());
        }

        Map<String, Object> errorResponse = createErrorResponse(errorMessage);
        errorResponse.put("proxyStatus", e.getRawStatusCode());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Crée une réponse d'erreur standardisée pour les proxies
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now());
        error.put("service", "notification-proxy");
        return error;
    }

    String communityServiceUrl = "lb://COMMUNITY-SERVICE";

// ============================================================================
// ROUTES POUR LES WEBSOCKETS - NOTIFICATIONS EN TEMPS RÉEL
// ============================================================================

    @GetMapping("/ws/**")
    public ResponseEntity<?> proxyWebSocketConnection(HttpServletRequest request) {
        // Les WebSockets sont généralement gérées différemment dans une architecture de microservices
        // Cette route peut servir pour la découverte du service WebSocket

        Map<String, Object> response = new HashMap<>();
        response.put("message", "WebSocket endpoint available");
        response.put("websocket_url", "ws://localhost:8080/ws"); // À adapter selon votre configuration
        response.put("endpoints", Arrays.asList(
                "/topic/notifications/{userId}",
                "/app/notifications/subscribe",
                "/app/notifications/test"
        ));
        response.put("service", "COMMUNITY-SERVICE");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour tester les notifications WebSocket via HTTP (pour développement)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/notifications/test")
    public ResponseEntity<?> proxyTestNotification(
            @RequestBody Map<String, String> testData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            // Validation des données de test
            String targetUserId = testData.get("userId");
            String message = testData.getOrDefault("message", "Notification de test");

            if (targetUserId == null || targetUserId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'ID utilisateur cible est requis"));
            }

            log.debug("Proxying test notification request from user: {} to user: {}", userId, targetUserId);
            String url = communityServiceUrl + "/api/notifications/test";

            HttpHeaders headers = createProxyHeaders(token);

            // Préparer les données pour le service
            Map<String, String> requestData = new HashMap<>();
            requestData.put("userId", targetUserId);
            requestData.put("message", message);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestData, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Object.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de l'envoi de notification de test: {}", e.getMessage());
            return handleProxyError(e, "Erreur lors de l'envoi de la notification de test");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de notification de test: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Endpoint pour gérer les souscriptions WebSocket via HTTP (fallback)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/notifications/subscribe")
    public ResponseEntity<?> proxyWebSocketSubscription(
            @RequestBody Map<String, String> subscriptionData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            String targetUserId = subscriptionData.get("userId");
            if (targetUserId == null || !targetUserId.equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Vous ne pouvez vous abonner qu'à vos propres notifications"));
            }

            log.debug("Proxying WebSocket subscription request for user: {}", userId);

            // Pour l'instant, retourner les informations de connexion WebSocket
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Souscription WebSocket initialisée");
            response.put("userId", userId);
            response.put("subscriptionTopic", "/topic/notifications/" + userId);
            response.put("timestamp", LocalDateTime.now());
            response.put("instructions", "Connectez-vous au WebSocket pour recevoir les notifications en temps réel");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la gestion de la souscription WebSocket pour l'utilisateur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Endpoint pour obtenir le statut de connexion WebSocket d'un utilisateur
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/notifications/{userId}/websocket-status")
    public ResponseEntity<?> proxyWebSocketStatus(
            @PathVariable String userId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String authenticatedUserId = (String) request.getAttribute("userId");
            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            if (!userId.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Accès non autorisé"));
            }

            log.debug("Checking WebSocket status for user: {}", userId);

            // Cette route pourrait être étendue pour vérifier le statut réel des connexions WebSocket
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("websocketAvailable", true);
            response.put("subscriptionTopic", "/topic/notifications/" + userId);
            response.put("connectionEndpoint", "/ws");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du statut WebSocket pour {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    /**
     * Endpoint pour créer une notification et la diffuser via WebSocket
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN') or #notificationData.userId == authentication.name")
    @PostMapping("/api/notifications/create")
    public ResponseEntity<?> proxyCreateNotification(
            @RequestBody Map<String, Object> notificationData,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        try {
            String userId = (String) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentification requise"));
            }

            // Validation des données
            String targetUserId = (String) notificationData.get("userId");
            String message = (String) notificationData.get("message");

            if (targetUserId == null || message == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("userId et message sont requis"));
            }

            log.debug("Proxying create notification request from user: {} for user: {}", userId, targetUserId);
            String url = communityServiceUrl + "/api/notifications/create";

            HttpHeaders headers = createProxyHeaders(token);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(notificationData, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Object.class);

            // Log de succès
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification créée avec succès et diffusée via WebSocket pour l'utilisateur: {}", targetUserId);
            }

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException e) {
            log.error("Erreur proxy lors de la création de notification: {}", e.getMessage());
            return handleProxyError(e, "Erreur lors de la création de la notification");
        } catch (Exception e) {
            log.error("Erreur inattendue lors du proxy de création de notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne du serveur"));
        }
    }

    // =============== MÉTHODES UTILITAIRES POUR WEBSOCKET ===============

    /**
     * Configuration des headers CORS pour WebSocket
     */
    private HttpHeaders createWebSocketHeaders(String token) {
        HttpHeaders headers = createProxyHeaders(token);
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        return headers;
    }

    /**
     * Endpoint de configuration WebSocket pour le frontend
     */
    @GetMapping("/api/websocket/config")
    public ResponseEntity<?> getWebSocketConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("websocketUrl", "ws://localhost:8080/ws"); // À adapter selon votre environnement
        config.put("topics", Map.of(
                "notifications", "/topic/notifications/{userId}",
                "broadcast", "/topic/broadcast"
        ));
        config.put("endpoints", Map.of(
                "subscribe", "/app/notifications/subscribe",
                "test", "/app/notifications/test"
        ));
        config.put("reconnection", Map.of(
                "enabled", true,
                "interval", 5000,
                "maxAttempts", 10
        ));

        return ResponseEntity.ok(config);
    }
}
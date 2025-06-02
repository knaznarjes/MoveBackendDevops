package com.move.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "routes")
public class RoutesConfig {
    private List<String> authenticated = new ArrayList<>();
    private List<String> admin = new ArrayList<>();
    private List<String> publicEndpoints = new ArrayList<>();
    private List<String> staticResources = new ArrayList<>();

    public List<String> getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(List<String> authenticated) {
        this.authenticated = authenticated;
    }

    public List<String> getAdmin() {
        return admin;
    }

    public void setAdmin(List<String> admin) {
        this.admin = admin;
    }

    public List<String> getPublic() {
        return publicEndpoints;
    }

    public void setPublic(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }

    public List<String> getStatic() {
        return staticResources;
    }

    public void setStatic(List<String> staticResources) {
        this.staticResources = staticResources;
    }
}
package com.move.contentservice.controller;

import com.move.contentservice.dto.LocationDTO;
import com.move.contentservice.service.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // ✅ Créer un lieu — tous les rôles peuvent le faire
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping
    public ResponseEntity<LocationDTO> createLocation(@RequestBody LocationDTO locationDTO) {
        return ResponseEntity.ok(locationService.createLocation(locationDTO));
    }

    // ✅ Lire un lieu par ID
    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable String id) {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    // ✅ Lire tous les lieux (liste complète) — seulement ADMIN & MASTERADMIN
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTERADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    // ✅ Mise à jour — tous les rôles (mais en vrai projet tu peux filtrer par propriété)
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<LocationDTO> updateLocation(@PathVariable String id,
                                                      @RequestBody LocationDTO locationDTO) {
        return ResponseEntity.ok(locationService.updateLocation(id, locationDTO));
    }

    // ✅ Suppression — tous les rôles (à filtrer si tu veux plus de contrôle)
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable String id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Recherche par pays
    @GetMapping("/country")
    public ResponseEntity<List<LocationDTO>> getByCountry(@RequestParam String country) {
        return ResponseEntity.ok(locationService.getLocationsByCountry(country));
    }


}

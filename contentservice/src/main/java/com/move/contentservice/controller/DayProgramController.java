package com.move.contentservice.controller;

import com.move.contentservice.dto.DayProgramDTO;
import com.move.contentservice.service.DayProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day-programs")  // Updated from "/api/itinerary-days"
@RequiredArgsConstructor
public class DayProgramController {

    private final DayProgramService dayProgramService;  // Updated from itineraryDayService

    // ✅ Création d'un jour — tous les rôles
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping
    public ResponseEntity<DayProgramDTO> create(@RequestBody DayProgramDTO dto) {
        return ResponseEntity.ok(dayProgramService.create(dto));
    }

    // ✅ Lecture par ID
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DayProgramDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(dayProgramService.getById(id));
    }

    // ✅ Tous les jours d'un contenu donné
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<DayProgramDTO>> getAllByContent(@PathVariable String contentId) {
        return ResponseEntity.ok(dayProgramService.getAllByContentId(contentId));
    }

    // ✅ Mise à jour
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DayProgramDTO> update(@PathVariable String id,
                                                @RequestBody DayProgramDTO dto) {
        return ResponseEntity.ok(dayProgramService.update(id, dto));
    }

    // ✅ Suppression
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        dayProgramService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
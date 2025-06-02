package com.move.contentservice.controller;

import com.move.contentservice.dto.ActivityPointDTO;
import com.move.contentservice.service.ActivityPointService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-points")
@RequiredArgsConstructor
public class ActivityPointController {

    private final ActivityPointService activityPointService;

    // EXISTING ENDPOINTS

    // ✅ Créer une activité
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PostMapping
    public ResponseEntity<ActivityPointDTO> create(@RequestBody ActivityPointDTO dto) {
        return ResponseEntity.ok(activityPointService.create(dto));
    }

    // ✅ Lire une activité par ID
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ActivityPointDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(activityPointService.getById(id));
    }

    // ✅ Lire toutes les activités liées à une journée
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/day-program/{dayProgramId}")
    public ResponseEntity<List<ActivityPointDTO>> getAllByDayProgram(@PathVariable String dayProgramId) {
        return ResponseEntity.ok(activityPointService.getAllByDayProgramId(dayProgramId));
    }


    // ✅ Mise à jour
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ActivityPointDTO> update(@PathVariable String id,
                                                   @RequestBody ActivityPointDTO dto) {
        return ResponseEntity.ok(activityPointService.update(id, dto));
    }

    // ✅ Suppression
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        activityPointService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // NEW GET ENDPOINTS

    // ✅ Get all activity points
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping
    public ResponseEntity<List<ActivityPointDTO>> getAll() {
        return ResponseEntity.ok(activityPointService.getAll());
    }

    // ✅ Get activity points with pagination
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/paginated")
    public ResponseEntity<Page<ActivityPointDTO>> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(activityPointService.getAllPaginated(pageable));
    }

    // ✅ Find by name
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/search/name")
    public ResponseEntity<List<ActivityPointDTO>> findByName(@RequestParam String name) {
        return ResponseEntity.ok(activityPointService.findByNameContaining(name));
    }



    // ✅ Find by location
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/search/location")
    public ResponseEntity<List<ActivityPointDTO>> findByLocation(@RequestParam String location) {
        return ResponseEntity.ok(activityPointService.findByLocationContaining(location));
    }

    // ✅ Find by max cost
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/search/cost")
    public ResponseEntity<List<ActivityPointDTO>> findByCostLessThanEqual(@RequestParam Double maxCost) {
        return ResponseEntity.ok(activityPointService.findByCostLessThanEqual(maxCost));
    }

    // ✅ Find by content ID
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<ActivityPointDTO>> findByContentId(@PathVariable String contentId) {
        return ResponseEntity.ok(activityPointService.findByContentId(contentId));
    }

    // ✅ Advanced search with multiple criteria
    @PreAuthorize("hasAnyRole('TRAVELER', 'ADMIN', 'MASTERADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<ActivityPointDTO>> searchActivityPoints(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double maxCost) {

        return ResponseEntity.ok(activityPointService.searchActivityPoints(name, type, location, maxCost));
    }

}
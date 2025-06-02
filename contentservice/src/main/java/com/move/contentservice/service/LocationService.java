package com.move.contentservice.service;

import com.move.contentservice.dto.LocationDTO;
import com.move.contentservice.exception.ResourceNotFoundException;
import com.move.contentservice.mapper.LocationMapper;
import com.move.contentservice.model.Location;
import com.move.contentservice.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Autowired
    public LocationService(LocationRepository locationRepository, LocationMapper locationMapper) {
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
    }

    public List<LocationDTO> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(locationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public LocationDTO getLocationById(String id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));
        return locationMapper.toDTO(location);
    }

    @Transactional
    public LocationDTO createLocation(LocationDTO locationDTO) {
        Location location = locationMapper.toEntity(locationDTO);
        Location savedLocation = locationRepository.save(location);
        return locationMapper.toDTO(savedLocation);
    }

    @Transactional
    public LocationDTO updateLocation(String id, LocationDTO locationDTO) {
        // Check if location exists
        locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));

        // Update location
        locationDTO.setId(id);
        Location updatedLocation = locationRepository.save(locationMapper.toEntity(locationDTO));
        return locationMapper.toDTO(updatedLocation);
    }

    @Transactional
    public void deleteLocation(String id) {
        // Check if location exists
        locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));

        locationRepository.deleteById(id);
    }

    public List<LocationDTO> getLocationsByCountry(String country) {
        List<Location> locations = locationRepository.findByCountry(country);
        return locations.stream()
                .map(locationMapper::toDTO)
                .collect(Collectors.toList());
    }


}
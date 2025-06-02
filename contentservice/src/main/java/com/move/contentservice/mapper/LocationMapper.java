package com.move.contentservice.mapper;

import com.move.contentservice.dto.LocationDTO;
import com.move.contentservice.model.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationDTO toDTO(Location location) {
        if (location == null) {
            return null;
        }

        return LocationDTO.builder()
                .id(location.getId())
                .address(location.getAddress())
                .country(location.getCountry())
                .lat(location.getLat())
                .lon(location.getLon())
                .build();

    }

    public Location toEntity(LocationDTO dto) {
        if (dto == null) {
            return null;
        }

        return Location.builder()
                .id(dto.getId())
                .address(dto.getAddress())
                .country(dto.getCountry())
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();

    }
}
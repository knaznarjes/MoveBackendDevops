package com.move.contentservice.repository;

import com.move.contentservice.model.Location;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LocationRepository extends MongoRepository<Location, String> {
    List<Location> findByCountry(String country);
}

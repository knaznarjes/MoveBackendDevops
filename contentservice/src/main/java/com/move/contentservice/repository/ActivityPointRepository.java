package com.move.contentservice.repository;

import com.move.contentservice.model.ActivityPoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ActivityPointRepository extends MongoRepository<ActivityPoint, String> {

    List<ActivityPoint> findByDayProgramId(String dayProgramId);
    List<ActivityPoint> findByDayProgramIdIn(List<String> dayProgramIds);

    @Query("{ " +
            "'name': { $regex: ?0, $options: 'i' }, " +
            "'type': { $regex: ?1, $options: 'i' }, " +
            "'location': { $regex: ?2, $options: 'i' }, " +
            "'cost': { $lte: ?3 } " +
            "}")
    List<ActivityPoint> findByCustomCriteria(String name, String type, String location, Double maxCost);

    List<ActivityPoint> findByCostLessThanEqual(Double maxCost);

    List<ActivityPoint> findByLocationContainingIgnoreCase(String location);

    List<ActivityPoint> findByNameContainingIgnoreCase(String name);
}

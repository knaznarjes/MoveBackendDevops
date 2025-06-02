package com.move.contentservice.repository;

import com.move.contentservice.model.DayProgram;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DayProgramRepository extends MongoRepository<DayProgram, String> {
    List<DayProgram> findByContentId(String contentId);
}

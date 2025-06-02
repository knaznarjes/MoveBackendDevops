package com.move.contentservice.service;

import com.move.contentservice.dto.ActivityPointDTO;
import com.move.contentservice.exception.ResourceNotFoundException;
import com.move.contentservice.mapper.ActivityPointMapper;
import com.move.contentservice.model.ActivityPoint;
import com.move.contentservice.model.DayProgram;
import com.move.contentservice.repository.ActivityPointRepository;
import com.move.contentservice.repository.DayProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityPointService {

    private final ActivityPointRepository activityPointRepository;
    private final DayProgramRepository dayProgramRepository;
    private final ActivityPointMapper activityPointMapper;

    @Autowired
    public ActivityPointService(ActivityPointRepository activityPointRepository,
                                DayProgramRepository dayProgramRepository,
                                ActivityPointMapper activityPointMapper) {
        this.activityPointRepository = activityPointRepository;
        this.dayProgramRepository = dayProgramRepository;
        this.activityPointMapper = activityPointMapper;
    }

    public List<ActivityPointDTO> getAllByDayProgramId(String dayProgramId) {
        List<ActivityPoint> activities = activityPointRepository.findByDayProgramId(dayProgramId);
        return activities.stream().map(activityPointMapper::toDTO).collect(Collectors.toList());
    }

    public ActivityPointDTO getById(String id) {
        ActivityPoint activity = activityPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityPoint not found with id: " + id));
        return activityPointMapper.toDTO(activity);
    }

    @Transactional
    public ActivityPointDTO create(ActivityPointDTO dto) {
        dayProgramRepository.findById(dto.getDayProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("DayProgram not found with id: " + dto.getDayProgramId()));

        ActivityPoint activity = activityPointMapper.toEntity(dto);
        return activityPointMapper.toDTO(activityPointRepository.save(activity));
    }

    @Transactional
    public ActivityPointDTO update(String id, ActivityPointDTO dto) {
        activityPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityPoint not found with id: " + id));

        dto.setId(id);
        ActivityPoint updated = activityPointMapper.toEntity(dto);
        return activityPointMapper.toDTO(activityPointRepository.save(updated));
    }

    @Transactional
    public void delete(String id) {
        activityPointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityPoint not found with id: " + id));
        activityPointRepository.deleteById(id);
    }

    public Page<ActivityPointDTO> getAllPaginated(Pageable pageable) {
        return activityPointRepository.findAll(pageable)
                .map(activityPointMapper::toDTO);
    }

    public List<ActivityPointDTO> getAll() {
        return activityPointRepository.findAll().stream()
                .map(activityPointMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityPointDTO> findByNameContaining(String name) {
        return activityPointRepository.findByNameContainingIgnoreCase(name).stream()
                .map(activityPointMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityPointDTO> findByLocationContaining(String location) {
        return activityPointRepository.findByLocationContainingIgnoreCase(location).stream()
                .map(activityPointMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityPointDTO> findByCostLessThanEqual(Double maxCost) {
        return activityPointRepository.findByCostLessThanEqual(maxCost).stream()
                .map(activityPointMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityPointDTO> findByContentId(String contentId) {
        List<String> dayIds = dayProgramRepository.findByContentId(contentId)
                .stream()
                .map(DayProgram::getId)
                .collect(Collectors.toList());

        return activityPointRepository.findByDayProgramIdIn(dayIds).stream()
                .map(activityPointMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActivityPointDTO> searchActivityPoints(String name, String type,
                                                       String location, Double maxCost) {
        return activityPointRepository.findByCustomCriteria(name, type, location, maxCost).stream()
                .map(activityPointMapper::toDTO)
                .collect(Collectors.toList());
    }
}

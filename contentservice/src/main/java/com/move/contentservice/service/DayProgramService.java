package com.move.contentservice.service;

import com.move.contentservice.dto.DayProgramDTO;
import com.move.contentservice.exception.ResourceNotFoundException;
import com.move.contentservice.mapper.DayProgramMapper;
import com.move.contentservice.model.DayProgram;
import com.move.contentservice.repository.ContentRepository;
import com.move.contentservice.repository.DayProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DayProgramService {

    private final DayProgramRepository dayProgramRepository;  // Renamed from itineraryDayRepository
    private final ContentRepository contentRepository;
    private final DayProgramMapper dayProgramMapper;  // Renamed from itineraryDayMapper

    @Autowired
    public DayProgramService(DayProgramRepository dayProgramRepository,
                             ContentRepository contentRepository,
                             DayProgramMapper dayProgramMapper) {
        this.dayProgramRepository = dayProgramRepository;
        this.contentRepository = contentRepository;
        this.dayProgramMapper = dayProgramMapper;
    }

    public List<DayProgramDTO> getAllByContentId(String contentId) {
        List<DayProgram> days = dayProgramRepository.findByContentId(contentId);
        return days.stream().map(dayProgramMapper::toDTO).collect(Collectors.toList());
    }

    public DayProgramDTO getById(String id) {
        DayProgram day = dayProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DayProgram not found with id: " + id));  // Updated error message
        return dayProgramMapper.toDTO(day);
    }

    @Transactional
    public DayProgramDTO create(DayProgramDTO dto) {
        // Vérifie que le contenu existe
        contentRepository.findById(dto.getContentId())
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + dto.getContentId()));

        DayProgram day = dayProgramMapper.toEntity(dto);
        return dayProgramMapper.toDTO(dayProgramRepository.save(day));
    }

    @Transactional
    public DayProgramDTO update(String id, DayProgramDTO dto) {
        dayProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DayProgram not found with id: " + id));  // Updated error message

        dto.setId(id);
        return dayProgramMapper.toDTO(dayProgramRepository.save(dayProgramMapper.toEntity(dto)));
    }

    @Transactional
    public void delete(String id) {
        dayProgramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DayProgram not found with id: " + id));  // Updated error message
        dayProgramRepository.deleteById(id);
    }
}
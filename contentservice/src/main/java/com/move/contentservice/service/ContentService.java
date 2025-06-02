package com.move.contentservice.service;

import com.move.contentservice.dto.ContentDTO;
import com.move.contentservice.exception.ResourceNotFoundException;
import com.move.contentservice.mapper.ContentMapper;
import com.move.contentservice.model.Content;
import com.move.contentservice.model.ContentType;
import com.move.contentservice.model.DayProgram;
import com.move.contentservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final EventPublisherService eventPublisherService;
    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DayProgramRepository dayProgramRepository;

    @Autowired
    private ActivityPointRepository activityPointRepository;

    @Autowired
    public ContentService(ContentRepository contentRepository,
                          ContentMapper contentMapper,
                          EventPublisherService eventPublisherService) {
        this.contentRepository = contentRepository;
        this.contentMapper = contentMapper;
        this.eventPublisherService = eventPublisherService;
    }

    public List<ContentDTO> getAllContents() {
        return contentMapper.toDTOList(contentRepository.findAll());
    }

    public ContentDTO getContentById(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
        return contentMapper.toDTO(content);
    }

    @Transactional
    public ContentDTO createContent(ContentDTO contentDTO) {
        if (contentDTO.getCreationDate() == null) {
            contentDTO.setCreationDate(new Date());
        }
        contentDTO.setLastModified(new Date());
        if (contentDTO.getIsPublished() == null) {
            contentDTO.setIsPublished(false);
        }

        Content content = contentMapper.toEntity(contentDTO);
        Content savedContent = contentRepository.save(content);

        // Save media
        if (savedContent.getMedia() != null) {
            savedContent.getMedia().forEach(media -> {
                media.setContentId(savedContent.getId());
                mediaRepository.save(media);
            });
        }

        // Save locations
        if (savedContent.getLocations() != null) {
            savedContent.getLocations().forEach(location -> locationRepository.save(location));
        }

        // Save day programs and their activities
        if (savedContent.getDayPrograms() != null) {
            savedContent.getDayPrograms().forEach(day -> {
                day.setContentId(savedContent.getId());
                DayProgram savedDay = dayProgramRepository.save(day);

                if (day.getActivities() != null) {
                    day.getActivities().forEach(activity -> {
                        activity.setDayProgramId(savedDay.getId());
                        activityPointRepository.save(activity);
                    });
                }
            });
        }

        eventPublisherService.publishContentCreated(savedContent);
        return contentMapper.toDTO(savedContent);
    }

    @Transactional
    public ContentDTO updateContent(String id, ContentDTO contentDTO) {
        contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));

        contentDTO.setId(id);
        contentDTO.setLastModified(new Date());
        Content updatedContent = contentMapper.toEntity(contentDTO);

        // Save updated media
        if (updatedContent.getMedia() != null) {
            updatedContent.getMedia().forEach(media -> {
                media.setContentId(id);
                mediaRepository.save(media);
            });
        }

        // Save updated locations
        if (updatedContent.getLocations() != null) {
            updatedContent.getLocations().forEach(location -> locationRepository.save(location));
        }

        // Save updated day programs and their activities
        if (updatedContent.getDayPrograms() != null) {
            updatedContent.getDayPrograms().forEach(day -> {
                day.setContentId(id);
                DayProgram savedDay = dayProgramRepository.save(day);

                if (day.getActivities() != null) {
                    day.getActivities().forEach(activity -> {
                        activity.setDayProgramId(savedDay.getId());
                        activityPointRepository.save(activity);
                    });
                }
            });
        }

        Content savedContent = contentRepository.save(updatedContent);
        eventPublisherService.publishContentUpdated(savedContent);
        return contentMapper.toDTO(savedContent);
    }

    @Transactional
    public void deleteContent(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
        contentRepository.deleteById(id);
        eventPublisherService.publishContentDeleted(content);
    }

    public List<ContentDTO> getContentsByUserId(String userId) {
        List<Content> contents = contentRepository.findByUserId(userId);
        return contentMapper.toDTOList(contents);
    }

    public Page<ContentDTO> getContentsByUserIdPaginated(String userId, Pageable pageable) {
        Page<Content> contentPage = contentRepository.findByUserId(userId, pageable);
        return contentPage.map(contentMapper::toDTO);
    }

    public List<ContentDTO> getContentsByType(ContentType type, String userId) {
        List<Content> contents = contentRepository.findByTypeAndUserId(type, userId);
        return contentMapper.toDTOList(contents);
    }

    public List<ContentDTO> getPublishedContents() {
        List<Content> contents = contentRepository.findByIsPublishedTrue();
        return contentMapper.toDTOList(contents);
    }

    public List<ContentDTO> searchContentsByKeyword(String keyword) {
        List<Content> contents = contentRepository.searchByKeyword(keyword);
        return contentMapper.toDTOList(contents);
    }

    public List<ContentDTO> getContentsByCountry(String country) {
        List<Content> contents = contentRepository.findByLocationCountry(country);
        return contentMapper.toDTOList(contents);
    }

    public List<ContentDTO> getTopRatedContents(Pageable pageable) {
        List<Content> contents = contentRepository.findByIsPublishedTrueOrderByRatingDesc(pageable);
        return contentMapper.toDTOList(contents);
    }

    public List<ContentDTO> getContentsByBudget(Double maxBudget) {
        List<Content> contents = contentRepository.findByBudgetLessThanEqual(maxBudget);
        return contentMapper.toDTOList(contents);
    }

    public List<ContentDTO> getContentsByDuration(int maxDuration) {
        List<Content> contents = contentRepository.findByDurationLessThanEqual(maxDuration);
        return contentMapper.toDTOList(contents);
    }

    @Transactional
    public ContentDTO publishContent(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));

        content.setIsPublished(true);
        content.setLastModified(new Date());

        Content savedContent = contentRepository.save(content);
        eventPublisherService.publishContentStatusChanged(savedContent, "published");
        return contentMapper.toDTO(savedContent);
    }

    @Transactional
    public ContentDTO unpublishContent(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));

        content.setIsPublished(false);
        content.setLastModified(new Date());

        Content savedContent = contentRepository.save(content);
        eventPublisherService.publishContentStatusChanged(savedContent, "unpublished");
        return contentMapper.toDTO(savedContent);
    }




    @Transactional
    public ContentDTO likeContent(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
        content.setLikeCount(content.getLikeCount() + 1);
        Content saved = contentRepository.save(content);
        return contentMapper.toDTO(saved);
    }
    @Transactional
    public ContentDTO unlikeContent(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + id));
        int currentLikes = content.getLikeCount();
        if (currentLikes > 0) {
            content.setLikeCount(currentLikes - 1);
        }

        Content saved = contentRepository.save(content);
        return contentMapper.toDTO(saved);
    }


    @Transactional

    public List<String> getUserLikedContentIds(String userId) {
        return contentRepository.findByUserId(userId)
                .stream()
                .filter(content -> content.getLikeCount() > 0) // Option simplifiée, à ajuster selon le vrai modèle des likes
                .map(Content::getId)
                .toList();
    }
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Map<String, Serializable>> getUserRatings(String userId) {
        return (List<Map<String, Serializable>>) (List<?>) contentRepository.findByUserId(userId)
                .stream()
                .filter(content -> content.getRating() > 0)
                .map(content -> Map.of(
                        "contentId", content.getId(),
                        "value", content.getRating()
                ))
                .toList();
    }
    @Transactional
    public List<ContentDTO> getTopLikedContents(Pageable pageable) {
        Page<Content> page = contentRepository.findByIsPublishedTrueOrderByLikeCountDesc(pageable);
        return page.map(contentMapper::toDTO).getContent(); // ou `.toList()` si tu veux juste la liste
    }
    @Transactional

    public Content blockContentAsMasterAdmin(String contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        content.setIsPublished(false); // ✅ Corrigé

        log.info("🔒 MASTERADMIN blocked content ID: {}", contentId);

        return contentRepository.save(content);
    }
    @Transactional

    public Content blockContentAsAdmin(String contentId, String ownerRole) {
        if (!"TRAVELER".equalsIgnoreCase(ownerRole)) {
            throw new IllegalArgumentException("Admins cannot block content from role: " + ownerRole);
        }

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        content.setIsPublished(false); // ✅ Corrigé

        log.info("🔒 ADMIN blocked content ID: {}", contentId);

        return contentRepository.save(content);
    }
    @Transactional
    public Content approveContent(String id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        content.setIsPublished(true);
        return contentRepository.save(content);
    }


}
package com.move.searchrecommendationservice.listener;

import com.move.searchrecommendationservice.model.ContentDTO;
import com.move.searchrecommendationservice.service.SynchronizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentEventListener {

    private final SynchronizationService syncService;

    @RabbitListener(queues = "${content.events.content-created-queue}")
    public void handleContentCreated(ContentDTO dto) {
        log.info("📥 Received Content Created event: {}", dto.getId());
        syncService.sync(dto);
    }

    @RabbitListener(queues = "${content.events.content-updated-queue}")
    public void handleContentUpdated(ContentDTO dto) {
        log.info("📥 Received Content Updated event: {}", dto.getId());
        syncService.sync(dto);
    }

    @RabbitListener(queues = "${content.events.content-deleted-queue}")
    public void handleContentDeleted(ContentDTO dto) {
        log.info("🗑️ Received Content Deleted event: {}", dto.getId());
        syncService.delete(dto.getId());
    }
}
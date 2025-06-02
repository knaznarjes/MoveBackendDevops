package com.move.contentservice.listener;

import com.move.contentservice.dto.ContentDTO;
import com.move.contentservice.model.Content;
import com.move.contentservice.repository.ContentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ContentEventListener {

    private final ContentRepository contentRepository;

    public ContentEventListener(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @RabbitListener(queues = "content.created.queue")
    public void handleContentCreated(ContentDTO dto) {
        log.info("📨 Event reçu — ID: {}, Titre: {}", dto.getId(), dto.getTitle());
    }

    @RabbitListener(queues = "content.updated.queue")
    public void handleContentUpdatedEvent(ContentDTO dto) {
        log.info("📨 Event reçu — Type: updated, ID: {}, Titre: {}", dto.getId(), dto.getTitle());

        contentRepository.findById(dto.getId()).ifPresentOrElse(
                content -> log.info("📘 Content: {} — UPDATED", content.getTitle()),
                () -> log.warn("⚠️ No content found for ID: {}", dto.getId())
        );
    }

    @RabbitListener(queues = "content.deleted.queue")
    public void handleContentDeletedEvent(ContentDTO dto) {
        log.info("📨 Event reçu — Type: deleted, ID: {}, User ID: {}", dto.getId(), dto.getUserId());
        log.info("🗑️ Content deleted — ID: {}", dto.getId());
    }
}
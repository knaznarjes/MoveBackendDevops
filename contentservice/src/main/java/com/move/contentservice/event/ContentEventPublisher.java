// ✅ FIXED VERSION
// ContentEventPublisher.java — use ContentDTO instead of Map
package com.move.contentservice.event;

import com.move.contentservice.dto.ContentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${content.events.exchange}")
    private String exchange;

    @Value("${content.events.content-created-routing-key}")
    private String contentCreatedRoutingKey;

    public void publishContentCreated(ContentDTO dto) {
        rabbitTemplate.convertAndSend(exchange, contentCreatedRoutingKey, dto);
    }

    @Value("${content.events.content-updated-routing-key}")
    private String contentUpdatedRoutingKey;

    public void publishContentUpdated(ContentDTO dto) {
        rabbitTemplate.convertAndSend(exchange, contentUpdatedRoutingKey, dto);
    }

    @Value("${content.events.content-deleted-routing-key}")
    private String contentDeletedRoutingKey;

    public void publishContentDeleted(ContentDTO dto) {
        rabbitTemplate.convertAndSend(exchange, contentDeletedRoutingKey, dto);
    }

}
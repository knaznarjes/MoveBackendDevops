package com.move.contentservice.service;

import com.move.contentservice.model.Content;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EventPublisherService {

    private final AmqpTemplate amqpTemplate;

    @Value("${content.events.exchange}")
    private String exchange;

    @Value("${content.events.content-created-routing-key}")
    private String contentCreatedRoutingKey;

    @Value("${content.events.content-updated-routing-key}")
    private String contentUpdatedRoutingKey;

    @Value("${content.events.content-deleted-routing-key}")
    private String contentDeletedRoutingKey;

    public EventPublisherService(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void publishContentCreated(Content content) {
        sendEvent(content, "created", contentCreatedRoutingKey);
    }

    public void publishContentUpdated(Content content) {
        sendEvent(content, "updated", contentUpdatedRoutingKey);
    }

    public void publishContentDeleted(Content content) {
        sendEvent(content, "deleted", contentDeletedRoutingKey);
    }

    private void sendEvent(Content content, String operation, String routingKey) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", content.getId());
        event.put("userId", content.getUserId());
        event.put("operation", operation);
        amqpTemplate.convertAndSend(exchange, routingKey, event);
    }

    public void publishContentStatusChanged(Content savedContent, String blocked) {
    }
}

package com.move.searchrecommendationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${content.events.exchange}")
    private String contentExchangeName;

    @Value("${content.events.content-created-routing-key}")
    private String contentCreatedRoutingKey;

    @Value("${content.events.content-updated-routing-key}")
    private String contentUpdatedRoutingKey;

    @Value("${content.events.content-deleted-routing-key}")
    private String contentDeletedRoutingKey;

    @Value("${content.events.content-created-queue}")
    private String contentCreatedQueue;

    @Value("${content.events.content-updated-queue}")
    private String contentUpdatedQueue;

    @Value("${content.events.content-deleted-queue}")
    private String contentDeletedQueue;

    @Bean
    public TopicExchange contentExchange() {
        return new TopicExchange(contentExchangeName);
    }

    @Bean
    public Queue contentCreatedQueue() {
        return new Queue(contentCreatedQueue, true);
    }

    @Bean
    public Queue contentUpdatedQueue() {
        return new Queue(contentUpdatedQueue, true);
    }

    @Bean
    public Queue contentDeletedQueue() {
        return new Queue(contentDeletedQueue, true);
    }

    @Bean
    public Binding bindingContentCreated() {
        return BindingBuilder.bind(contentCreatedQueue())
                .to(contentExchange())
                .with(contentCreatedRoutingKey);
    }

    @Bean
    public Binding bindingContentUpdated() {
        return BindingBuilder.bind(contentUpdatedQueue())
                .to(contentExchange())
                .with(contentUpdatedRoutingKey);
    }

    @Bean
    public Binding bindingContentDeleted() {
        return BindingBuilder.bind(contentDeletedQueue())
                .to(contentExchange())
                .with(contentDeletedRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
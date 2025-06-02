package com.move.contentservice.config;

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
    private String exchange;

    @Value("${content.events.content-created-routing-key}")
    private String contentCreatedRoutingKey;

    @Value("${content.events.content-updated-routing-key}")
    private String contentUpdatedRoutingKey;

    @Value("${content.events.content-deleted-routing-key}")
    private String contentDeletedRoutingKey;

    @Bean
    public TopicExchange contentExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue contentCreatedQueue() {
        return new Queue("content.created.queue", true);
    }

    @Bean
    public Queue contentUpdatedQueue() {
        return new Queue("content.updated.queue", true);
    }

    @Bean
    public Queue contentDeletedQueue() {
        return new Queue("content.deleted.queue", true);
    }

    @Bean
    public Binding bindingCreatedQueue(Queue contentCreatedQueue, TopicExchange contentExchange) {
        return BindingBuilder.bind(contentCreatedQueue).to(contentExchange).with(contentCreatedRoutingKey);
    }

    @Bean
    public Binding bindingUpdatedQueue(Queue contentUpdatedQueue, TopicExchange contentExchange) {
        return BindingBuilder.bind(contentUpdatedQueue).to(contentExchange).with(contentUpdatedRoutingKey);
    }

    @Bean
    public Binding bindingDeletedQueue(Queue contentDeletedQueue, TopicExchange contentExchange) {
        return BindingBuilder.bind(contentDeletedQueue).to(contentExchange).with(contentDeletedRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}

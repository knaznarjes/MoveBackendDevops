package com.move.communitynotificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Noms des queues pour Community Service
    public static final String COMMENT_CREATED_QUEUE = "comment.created.queue";
    public static final String LIKE_CREATED_QUEUE = "like.created";
    public static final String FOLLOW_CREATED_QUEUE = "follow.created";

    // Noms des queues pour Content Service (si besoin d'écouter)
    public static final String CONTENT_CREATED_QUEUE = "content.created.queue";
    public static final String CONTENT_UPDATED_QUEUE = "content.updated.queue";
    public static final String CONTENT_DELETED_QUEUE = "content.deleted.queue";

    // Nom de l'exchange
    public static final String COMMUNITY_EXCHANGE = "community.exchange";
    public static final String CONTENT_EXCHANGE = "content.events.exchange";

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // ================== EXCHANGES ==================
    @Bean
    public TopicExchange communityExchange() {
        return new TopicExchange(COMMUNITY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange contentExchange() {
        return new TopicExchange(CONTENT_EXCHANGE, true, false);
    }

    // ================== QUEUES ==================
    @Bean
    public Queue commentCreatedQueue() {
        return QueueBuilder.durable(COMMENT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.community")
                .build();
    }

    @Bean
    public Queue likeCreatedQueue() {
        return QueueBuilder.durable(LIKE_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.community")
                .build();
    }

    @Bean
    public Queue followCreatedQueue() {
        return QueueBuilder.durable(FOLLOW_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.community")
                .build();
    }

    // Queues pour écouter les événements de contenu (optionnel)
    @Bean
    public Queue contentCreatedQueue() {
        return QueueBuilder.durable(CONTENT_CREATED_QUEUE).build();
    }

    @Bean
    public Queue contentUpdatedQueue() {
        return QueueBuilder.durable(CONTENT_UPDATED_QUEUE).build();
    }

    @Bean
    public Queue contentDeletedQueue() {
        return QueueBuilder.durable(CONTENT_DELETED_QUEUE).build();
    }

    // ================== BINDINGS ==================
    @Bean
    public Binding commentCreatedBinding() {
        return BindingBuilder
                .bind(commentCreatedQueue())
                .to(communityExchange())
                .with("comment.created");
    }

    @Bean
    public Binding likeCreatedBinding() {
        return BindingBuilder
                .bind(likeCreatedQueue())
                .to(communityExchange())
                .with("like.created");
    }

    @Bean
    public Binding followCreatedBinding() {
        return BindingBuilder
                .bind(followCreatedQueue())
                .to(communityExchange())
                .with("follow.created");
    }

    // Bindings pour les événements de contenu
    @Bean
    public Binding contentCreatedBinding() {
        return BindingBuilder
                .bind(contentCreatedQueue())
                .to(contentExchange())
                .with("content.created");
    }

    @Bean
    public Binding contentUpdatedBinding() {
        return BindingBuilder
                .bind(contentUpdatedQueue())
                .to(contentExchange())
                .with("content.updated");
    }

    @Bean
    public Binding contentDeletedBinding() {
        return BindingBuilder
                .bind(contentDeletedQueue())
                .to(contentExchange())
                .with("content.deleted");
    }

    // ================== DEAD LETTER EXCHANGE ==================
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.community");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlq.community").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("");
    }
}
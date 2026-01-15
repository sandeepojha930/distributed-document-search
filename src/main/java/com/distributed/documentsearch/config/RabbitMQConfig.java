package com.distributed.documentsearch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.ApplicationRunner;

import com.distributed.documentsearch.listener.DocumentIndexListener;

/**
 * Configuration class for RabbitMQ messaging infrastructure.
 *
 * This class sets up the message broker topology including exchanges, queues,
 * bindings, and listeners for asynchronous document processing. It implements
 * the messaging pattern for decoupling document creation from indexing operations.
 *
 * Topology:
 * - Topic exchange: document-exchange
 * - Queues: document.index, document.delete
 * - Routing keys: document.index.*, document.delete.*
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    /** Name of the topic exchange for document operations */
    public static final String DOCUMENT_EXCHANGE = "document-exchange";

    /** Name of the queue for document indexing operations */
    public static final String INDEX_QUEUE = "document.index";

    /** Name of the queue for document deletion operations */
    public static final String DELETE_QUEUE = "document.delete";

    /* ---------- Exchange ---------- */
    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange(DOCUMENT_EXCHANGE, true, false);
    }

    /* ---------- Queues ---------- */
    @Bean
    public Queue documentIndexQueue() {
        return QueueBuilder.durable(INDEX_QUEUE).build();
    }

    @Bean
    public Queue documentDeleteQueue() {
        return QueueBuilder.durable(DELETE_QUEUE).build();
    }

    /* ---------- Bindings (use wildcard routing) ---------- */
    @Bean
    public Binding indexBinding() {
        return BindingBuilder
                .bind(documentIndexQueue())
                .to(documentExchange())
                .with("document.index.*");
    }

    @Bean
    public Binding deleteBinding() {
        return BindingBuilder
                .bind(documentDeleteQueue())
                .to(documentExchange())
                .with("document.delete.*");
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    @DependsOn("rabbitAdmin")
    public ApplicationRunner rabbitInitializer(RabbitAdmin rabbitAdmin) {
        return args -> {
            // Force initialization of RabbitMQ declarations
            try {
                rabbitAdmin.initialize();
                log.info("RabbitMQ declarations initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize RabbitMQ declarations: {}", e.getMessage());
                throw e;
            }
        };
    }

    // Programmatic listener container to ensure messages are consumed
    @Bean
    @DependsOn("rabbitAdmin")
    public SimpleMessageListenerContainer indexListenerContainer(
            ConnectionFactory connectionFactory,
            DocumentIndexListener listener,
            Jackson2JsonMessageConverter converter) {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(INDEX_QUEUE);
        container.setMessageListener(new MessageListenerAdapter(listener, "handleIndexMessage"));
        ((MessageListenerAdapter) container.getMessageListener()).setMessageConverter(converter);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(1);

        log.info("Created programmatic RabbitMQ listener container for queue: {}", INDEX_QUEUE);
        return container;
    }
}

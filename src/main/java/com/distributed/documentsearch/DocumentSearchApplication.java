package com.distributed.documentsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * Main application class for the Distributed Document Search Service.
 *
 * This Spring Boot application provides a distributed document search system
 * with multi-tenancy support, full-text search capabilities, and asynchronous
 * document indexing using Elasticsearch, PostgreSQL, Redis, and RabbitMQ.
 *
 * Key Features:
 * - Multi-tenant document storage and search
 * - Full-text search with relevance ranking
 * - Asynchronous document indexing
 * - Redis caching for performance
 * - Rate limiting per tenant
 * - Fault tolerance with Circuit Breaker pattern
 * - Message-driven architecture with RabbitMQ
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableRabbit
@ComponentScan(basePackages = "com.distributed.documentsearch")
public class DocumentSearchApplication {

    /**
     * Main entry point for the Distributed Document Search application.
     *
     * Initializes and starts the Spring Boot application with all configured
     * components including web controllers, services, repositories, and
     * messaging infrastructure.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(DocumentSearchApplication.class, args);
    }
}

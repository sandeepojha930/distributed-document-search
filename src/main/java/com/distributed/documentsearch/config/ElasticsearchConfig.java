package com.distributed.documentsearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.distributed.documentsearch.repository")
public class ElasticsearchConfig {
    
    // Configuration handled by application.yml
    // Spring Boot 3.x auto-configures Elasticsearch client from spring.data.elasticsearch.uris
}

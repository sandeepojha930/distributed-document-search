package com.distributed.documentsearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    
    public Map<String, String> checkHealth() {
        Map<String, String> checks = new HashMap<>();
        
        // Check PostgreSQL
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            checks.put("postgresql", "UP");
        } catch (Exception e) {
            log.error("PostgreSQL health check failed", e);
            checks.put("postgresql", "DOWN");
        }
        
        // Check Elasticsearch
        try {
            elasticsearchOperations.indexOps(com.distributed.documentsearch.model.DocumentIndex.class).exists();
            checks.put("elasticsearch", "UP");
        } catch (Exception e) {
            log.error("Elasticsearch health check failed", e);
            checks.put("elasticsearch", "DOWN");
        }
        
        // Check Redis
        try {
            redisTemplate.opsForValue().get("health-check");
            redisTemplate.opsForValue().set("health-check", "ok", java.time.Duration.ofSeconds(1));
            checks.put("redis", "UP");
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            checks.put("redis", "DOWN");
        }
        
        // Check RabbitMQ connection
        try {
            // Simple check - try to get connection factory and verify it's available
            var connectionFactory = rabbitTemplate.getConnectionFactory();
            if (connectionFactory != null) {
                var connection = connectionFactory.createConnection();
                if (connection != null && connection.isOpen()) {
                    checks.put("rabbitmq", "UP");
                    connection.close();
                } else {
                    checks.put("rabbitmq", "DOWN");
                }
            } else {
                checks.put("rabbitmq", "DOWN");
            }
        } catch (Exception e) {
            log.error("RabbitMQ health check failed", e);
            checks.put("rabbitmq", "DOWN");
        }
        
        return checks;
    }
    
    public String getOverallStatus(Map<String, String> checks) {
        boolean allUp = checks.values().stream().allMatch("UP"::equals);
        return allUp ? "UP" : "DOWN";
    }
}

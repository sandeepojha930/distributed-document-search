package com.distributed.documentsearch.controller;

import com.distributed.documentsearch.dto.HealthResponse;
import com.distributed.documentsearch.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for system health monitoring.
 *
 * This controller provides health check endpoints to monitor the status
 * of all system components including databases, search engines, and
 * message queues.
 *
 * Base path: /api/v1/health
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    /** Service for performing health checks on system components */
    private final HealthService healthService;

    /**
     * Performs comprehensive health checks on all system components.
     *
     * This endpoint checks the connectivity and operational status of all
     * critical system dependencies including PostgreSQL, Elasticsearch,
     * Redis, and RabbitMQ.
     *
     * @return ResponseEntity containing health status and component checks
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        Map<String, String> checks = healthService.checkHealth();
        String status = healthService.getOverallStatus(checks);

        HealthResponse response = HealthResponse.builder()
                .status(status)
                .checks(checks)
                .build();

        return ResponseEntity.ok(response);
    }
}

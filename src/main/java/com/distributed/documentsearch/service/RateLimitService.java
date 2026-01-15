package com.distributed.documentsearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    @Value("${app.rate-limit.window-size-seconds:60}")
    private int windowSizeSeconds;
    
    public boolean isAllowed(String tenantId) {
        if (!rateLimitEnabled) {
            return true;
        }
        
        String key = "ratelimit:" + tenantId + ":" + (System.currentTimeMillis() / (windowSizeSeconds * 1000));
        
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));
            }
            
            boolean allowed = count <= requestsPerMinute;
            
            if (!allowed) {
                log.warn("Rate limit exceeded for tenant: {}", tenantId);
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("Error checking rate limit for tenant: {}", tenantId, e);
            // Fail open - allow request if Redis is down
            return true;
        }
    }
}

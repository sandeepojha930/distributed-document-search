package com.distributed.documentsearch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimitService = new RateLimitService(redisTemplate);

        // Manually configure fields via reflection
        setField("rateLimitEnabled", true);
        setField("requestsPerMinute", 2);
        setField("windowSizeSeconds", 60);
    }

    private void setField(String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = RateLimitService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(rateLimitService, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
    }
    }

    @Test
    void isAllowed_allowsRequestsWithinLimit() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 2L);

        boolean first = rateLimitService.isAllowed("tenant-1");
        boolean second = rateLimitService.isAllowed("tenant-1");

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        verify(redisTemplate, atLeastOnce()).expire(anyString(), any(Duration.class));
    }

    @Test
    void isAllowed_blocksWhenLimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(3L);

        boolean allowed = rateLimitService.isAllowed("tenant-1");

        assertThat(allowed).isFalse();
    }

    @Test
    void isAllowed_failsOpenWhenRedisThrows() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

        boolean allowed = rateLimitService.isAllowed("tenant-1");

        assertThat(allowed).isTrue();
    }
}


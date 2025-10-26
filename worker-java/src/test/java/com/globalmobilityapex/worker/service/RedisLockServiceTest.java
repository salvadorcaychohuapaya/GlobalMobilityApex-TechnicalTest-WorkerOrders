package com.globalmobilityapex.worker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private RedisLockService redisLockService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisLockService = new RedisLockService(redisTemplate, 30000L, 100L);
    }

    @Test
    void testAcquireLock_Success() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(redisLockService.acquireLock("customer-1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testAcquireLock_AlreadyLocked() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(false));

        StepVerifier.create(redisLockService.acquireLock("customer-1"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testReleaseLock_Success() {
        when(valueOperations.delete(anyString()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(redisLockService.releaseLock("customer-1"))
                .expectNext(true)
                .verifyComplete();
    }
}
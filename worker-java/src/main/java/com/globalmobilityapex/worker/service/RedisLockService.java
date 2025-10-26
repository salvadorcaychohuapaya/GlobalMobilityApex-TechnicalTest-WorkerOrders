package com.globalmobilityapex.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class RedisLockService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final long lockTimeout;
    private final long retryInterval;

    public RedisLockService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${redis.lock.timeout}") long lockTimeout,
            @Value("${redis.lock.retry-interval}") long retryInterval
    ) {
        this.redisTemplate = redisTemplate;
        this.lockTimeout = lockTimeout;
        this.retryInterval = retryInterval;
        log.info("RedisLockService initialized - Timeout: {}ms, Retry: {}ms", lockTimeout, retryInterval);
    }

    public Mono<Boolean> acquireLock(String customerId) {
        String lockKey = "lock:customer:" + customerId;
        String lockValue = String.valueOf(System.currentTimeMillis());

        log.debug("Attempting to acquire lock for customer: {}", customerId);

        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofMillis(lockTimeout))
                .doOnSuccess(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        log.info("Lock acquired for customer: {}", customerId);
                    } else {
                        log.warn("Lock already exists for customer: {}", customerId);
                    }
                })
                .doOnError(error -> 
                    log.error("Error acquiring lock for customer {}: {}", customerId, error.getMessage())
                )
                .onErrorReturn(false);
    }

    public Mono<Boolean> releaseLock(String customerId) {
        String lockKey = "lock:customer:" + customerId;

        log.debug("Releasing lock for customer: {}", customerId);

        return redisTemplate.opsForValue()
                .delete(lockKey)
                .doOnSuccess(deleted -> {
                    if (Boolean.TRUE.equals(deleted)) {
                        log.info("Lock released for customer: {}", customerId);
                    } else {
                        log.warn("No lock found to release for customer: {}", customerId);
                    }
                })
                .doOnError(error -> 
                    log.error("Error releasing lock for customer {}: {}", customerId, error.getMessage())
                )
                .onErrorReturn(false);
    }

    public Mono<Boolean> acquireLockWithRetry(String customerId, int maxRetries) {
        return Mono.defer(() -> acquireLock(customerId))
                .flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return Mono.just(true);
                    }
                    
                    if (maxRetries > 0) {
                        log.debug("Retrying lock acquisition for customer: {} (retries left: {})", 
                            customerId, maxRetries);
                        return Mono.delay(Duration.ofMillis(retryInterval))
                                .then(acquireLockWithRetry(customerId, maxRetries - 1));
                    }
                    
                    log.error("Failed to acquire lock for customer: {} after all retries", customerId);
                    return Mono.just(false);
                });
    }
}
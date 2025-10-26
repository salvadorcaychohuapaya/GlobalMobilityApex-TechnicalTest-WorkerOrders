package com.globalmobilityapex.worker.service;

import com.globalmobilityapex.worker.model.Customer;
import com.globalmobilityapex.worker.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
public class ExternalApiService {

    private final WebClient webClient;
    private final int maxRetries;
    private final long backoffDelay;

    public ExternalApiService(
            @Value("${external.api.base-url}") String baseUrl,
            @Value("${external.api.timeout}") int timeout,
            @Value("${external.api.retry.max-attempts}") int maxRetries,
            @Value("${external.api.retry.backoff-delay}") long backoffDelay
    ) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        log.info("ExternalApiService initialized - Base URL: {}, Timeout: {}ms", baseUrl, timeout);
    }

    public Mono<Product> getProduct(String productId) {
        log.debug("Fetching product: {}", productId);

        return webClient.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(Product.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(backoffDelay))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                        .doBeforeRetry(signal -> 
                            log.warn("Retrying product fetch for: {} (attempt: {})", 
                                productId, signal.totalRetries() + 1)
                        )
                )
                .doOnSuccess(product -> 
                    log.info("Product fetched successfully: {} - {}", productId, product.getName())
                )
                .doOnError(error -> 
                    log.error("Error fetching product {}: {}", productId, error.getMessage())
                )
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.error("Product not found: {}", productId);
                    return Mono.error(new RuntimeException("Product not found: " + productId));
                });
    }

    public Mono<Customer> getCustomer(String customerId) {
        log.debug("Fetching customer: {}", customerId);

        return webClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(Customer.class)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(backoffDelay))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                        .doBeforeRetry(signal -> 
                            log.warn("Retrying customer fetch for: {} (attempt: {})", 
                                customerId, signal.totalRetries() + 1)
                        )
                )
                .doOnSuccess(customer -> 
                    log.info("Customer fetched successfully: {} - {}", customerId, customer.getName())
                )
                .doOnError(error -> 
                    log.error("Error fetching customer {}: {}", customerId, error.getMessage())
                )
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.error("Customer not found: {}", customerId);
                    return Mono.error(new RuntimeException("Customer not found: " + customerId));
                });
    }
}
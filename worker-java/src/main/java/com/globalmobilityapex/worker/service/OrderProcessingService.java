package com.globalmobilityapex.worker.service;

import com.globalmobilityapex.worker.model.*;
import com.globalmobilityapex.worker.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final ExternalApiService externalApiService;
    private final RedisLockService redisLockService;
    private final OrderRepository orderRepository;

    public Mono<Order> processOrder(OrderMessage orderMessage) {
        String orderId = orderMessage.getOrderId();
        String customerId = orderMessage.getCustomerId();

        log.info("Processing order: {} for customer: {}", orderId, customerId);

        return redisLockService.acquireLockWithRetry(customerId, 3)
                .flatMap(lockAcquired -> {
                    if (!lockAcquired) {
                        log.error("Failed to acquire lock for customer: {}", customerId);
                        return Mono.error(new RuntimeException(
                            "Could not acquire lock for customer: " + customerId
                        ));
                    }

                    log.info("Lock acquired for customer: {}", customerId);

                    return externalApiService.getCustomer(customerId)
                            .flatMap(customer -> {
                                if (!customer.getActive()) {
                                    log.error("Customer is not active: {}", customerId);
                                    return releaseLockAndFail(customerId, 
                                        "Customer is not active: " + customerId);
                                }

                                log.info("Customer validated: {} - {}", customerId, customer.getName());

                                return fetchProducts(orderMessage.getProductIds())
                                        .collectList()
                                        .flatMap(products -> {
                                            if (products.size() != orderMessage.getProductIds().size()) {
                                                log.error("Not all products found for order: {}", orderId);
                                                return releaseLockAndFail(customerId, 
                                                    "Not all products found");
                                            }

                                            List<OrderItem> items = createOrderItems(products);
                                            Double totalAmount = calculateTotal(items);

                                            Order order = Order.builder()
                                                    .orderId(orderId)
                                                    .customerId(customerId)
                                                    .customerName(customer.getName())
                                                    .customerEmail(customer.getEmail())
                                                    .items(items)
                                                    .totalAmount(totalAmount)
                                                    .status("COMPLETED")
                                                    .createdAt(LocalDateTime.now())
                                                    .updatedAt(LocalDateTime.now())
                                                    .build();

                                            return orderRepository.save(order)
                                                    .doOnSuccess(savedOrder -> 
                                                        log.info("Order saved successfully: {} (Total: ${})", 
                                                            orderId, totalAmount)
                                                    )
                                                    .doOnError(error -> 
                                                        log.error("Error saving order {}: {}", 
                                                            orderId, error.getMessage())
                                                    );
                                        });
                            })
                            .doFinally(signalType -> {
                                redisLockService.releaseLock(customerId)
                                        .subscribe(
                                            released -> log.debug("Lock released for customer: {}", customerId),
                                            error -> log.error("Error releasing lock for customer {}: {}", 
                                                customerId, error.getMessage())
                                        );
                            });
                })
                .doOnError(error -> 
                    log.error("Error processing order {}: {}", orderId, error.getMessage())
                );
    }

    private Flux<Product> fetchProducts(List<String> productIds) {
        log.debug("Fetching {} products", productIds.size());

        return Flux.fromIterable(productIds)
                .flatMap(productId -> 
                    externalApiService.getProduct(productId)
                            .onErrorResume(error -> {
                                log.error("Error fetching product {}: {}", productId, error.getMessage());
                                return Mono.empty();
                            })
                );
    }

    private List<OrderItem> createOrderItems(List<Product> products) {
        return products.stream()
                .map(product -> {
                    int quantity = 1;
                    double subtotal = product.getPrice() * quantity;

                    return OrderItem.builder()
                            .productId(product.getProductId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .quantity(quantity)
                            .subtotal(subtotal)
                            .build();
                })
                .toList();
    }

    private Double calculateTotal(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }

    private Mono<Order> releaseLockAndFail(String customerId, String errorMessage) {
        return redisLockService.releaseLock(customerId)
                .then(Mono.error(new RuntimeException(errorMessage)));
    }
}
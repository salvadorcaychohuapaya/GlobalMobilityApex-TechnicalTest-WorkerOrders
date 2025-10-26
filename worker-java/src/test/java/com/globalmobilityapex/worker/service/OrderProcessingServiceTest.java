package com.globalmobilityapex.worker.service;

import com.globalmobilityapex.worker.model.*;
import com.globalmobilityapex.worker.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private OrderRepository orderRepository;

    private OrderProcessingService orderProcessingService;

    @BeforeEach
    void setUp() {
        orderProcessingService = new OrderProcessingService(
                externalApiService, redisLockService, orderRepository);
    }

    @Test
    void testProcessOrder_Success() {
        OrderMessage orderMessage = new OrderMessage(
                "order-1",
                "customer-1",
                Arrays.asList("product-1", "product-2")
        );

        Customer customer = new Customer();
        customer.setCustomerId("customer-1");
        customer.setName("Juan Perez");
        customer.setEmail("juan@example.com");
        customer.setActive(true);

        Product product1 = new Product();
        product1.setProductId("product-1");
        product1.setName("Laptop");
        product1.setPrice(999.99);

        Product product2 = new Product();
        product2.setProductId("product-2");
        product2.setName("Mouse");
        product2.setPrice(29.99);

        Order savedOrder = Order.builder()
                .orderId("order-1")
                .customerId("customer-1")
                .totalAmount(1029.98)
                .status("COMPLETED")
                .build();

        when(redisLockService.acquireLockWithRetry(anyString(), any(Integer.class)))
                .thenReturn(Mono.just(true));
        when(externalApiService.getCustomer("customer-1"))
                .thenReturn(Mono.just(customer));
        when(externalApiService.getProduct("product-1"))
                .thenReturn(Mono.just(product1));
        when(externalApiService.getProduct("product-2"))
                .thenReturn(Mono.just(product2));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.just(savedOrder));
        when(redisLockService.releaseLock(anyString()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(orderProcessingService.processOrder(orderMessage))
                .assertNext(order -> {
                    assertEquals("order-1", order.getOrderId());
                    assertEquals("COMPLETED", order.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void testProcessOrder_CustomerInactive() {
        OrderMessage orderMessage = new OrderMessage(
                "order-2",
                "customer-3",
                Arrays.asList("product-1")
        );

        Customer inactiveCustomer = new Customer();
        inactiveCustomer.setCustomerId("customer-3");
        inactiveCustomer.setName("Pedro Lopez");
        inactiveCustomer.setActive(false);

        when(redisLockService.acquireLockWithRetry(anyString(), any(Integer.class)))
                .thenReturn(Mono.just(true));
        when(externalApiService.getCustomer("customer-3"))
                .thenReturn(Mono.just(inactiveCustomer));
        when(redisLockService.releaseLock(anyString()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(orderProcessingService.processOrder(orderMessage))
                .expectErrorMessage("Customer is not active: customer-3")
                .verify();
    }
}
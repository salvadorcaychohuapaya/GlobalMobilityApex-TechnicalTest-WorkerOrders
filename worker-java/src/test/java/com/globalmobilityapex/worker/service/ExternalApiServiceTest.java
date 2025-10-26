package com.globalmobilityapex.worker.service;

import com.globalmobilityapex.worker.model.Customer;
import com.globalmobilityapex.worker.model.Product;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ExternalApiServiceTest {

    private MockWebServer mockWebServer;
    private ExternalApiService externalApiService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        externalApiService = new ExternalApiService(baseUrl, 5000, 3, 1000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGetProduct_Success() {
        String productJson = """
            {
                "productId": "product-1",
                "name": "Laptop",
                "price": 999.99,
                "active": true
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(productJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(externalApiService.getProduct("product-1"))
                .assertNext(product -> {
                    assertEquals("product-1", product.getProductId());
                    assertEquals("Laptop", product.getName());
                    assertEquals(999.99, product.getPrice());
                })
                .verifyComplete();
    }

    @Test
    void testGetProduct_NotFound() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(externalApiService.getProduct("product-999"))
                .expectErrorMessage("Product not found: product-999")
                .verify();
    }

    @Test
    void testGetCustomer_Success() {
        String customerJson = """
            {
                "customerId": "customer-1",
                "name": "Juan Perez",
                "email": "juan@example.com",
                "active": true
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(customerJson)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(externalApiService.getCustomer("customer-1"))
                .assertNext(customer -> {
                    assertEquals("customer-1", customer.getCustomerId());
                    assertEquals("Juan Perez", customer.getName());
                    assertTrue(customer.getActive());
                })
                .verifyComplete();
    }
}
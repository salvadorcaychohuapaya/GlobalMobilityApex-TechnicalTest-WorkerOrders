package com.globalmobilityapex.worker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    
    @Id
    private String id;
    
    private String orderId;
    private String customerId;
    private String customerName;
    private String customerEmail;
    
    private List<OrderItem> items;
    private Double totalAmount;
    
    private String status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
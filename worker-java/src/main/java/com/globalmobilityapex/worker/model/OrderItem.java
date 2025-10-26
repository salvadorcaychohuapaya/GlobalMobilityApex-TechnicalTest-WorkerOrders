package com.globalmobilityapex.worker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    private String productId;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private Double subtotal;
}
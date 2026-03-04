package com.example.fledemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String tenantId;
    private String orderId;
    private String customerId;
    private String product;
    private Double amount;
    private String status;
}


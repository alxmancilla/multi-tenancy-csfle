package com.example.fledemo.dto;

import com.example.fledemo.model.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String tenantId;
    private String orderId;
    private String customerId;
    private String product;
    private Double amount;
    private String status;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .tenantId(order.getTenantId())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .product(order.getProduct())
                .amount(order.getAmount())
                .status(order.getStatus())
                .build();
    }
}


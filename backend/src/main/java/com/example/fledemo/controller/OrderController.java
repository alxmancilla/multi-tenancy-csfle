package com.example.fledemo.controller;

import com.example.fledemo.dto.OrderRequest;
import com.example.fledemo.dto.OrderResponse;
import com.example.fledemo.model.Order;
import com.example.fledemo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable String tenantId,
            @RequestBody OrderRequest request) {
        log.info("Creating order for tenant: {}", tenantId);
        
        Order order = orderService.createOrder(
                tenantId,
                request.getCustomerId(),
                request.getProduct(),
                request.getAmount(),
                request.getStatus()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@PathVariable String tenantId) {
        log.info("Fetching orders for tenant: {}", tenantId);
        
        List<OrderResponse> orders = orderService.getOrdersByTenant(tenantId)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String tenantId,
            @PathVariable String customerId) {
        log.info("Fetching orders for customer {} in tenant: {}", customerId, tenantId);
        
        List<OrderResponse> orders = orderService.getOrdersByCustomer(tenantId, customerId)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(orders);
    }
}


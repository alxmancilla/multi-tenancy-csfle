package com.example.fledemo.controller;

import com.example.fledemo.dto.CustomerRequest;
import com.example.fledemo.dto.CustomerResponse;
import com.example.fledemo.model.Customer;
import com.example.fledemo.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @PathVariable String tenantId,
            @RequestBody CustomerRequest request) {
        log.info("Creating customer for tenant: {}", tenantId);
        
        Customer customer = customerService.createCustomer(
                tenantId,
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getAddress()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customer));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getCustomers(@PathVariable String tenantId) {
        log.info("Fetching customers for tenant: {}", tenantId);
        
        List<CustomerResponse> customers = customerService.getCustomersByTenant(tenantId)
                .stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/search")
    public ResponseEntity<CustomerResponse> searchByEmail(
            @PathVariable String tenantId,
            @RequestParam String email) {
        log.info("Searching for customer by email in tenant: {}", tenantId);
        
        Customer customer = customerService.getCustomerByEmail(tenantId, email);
        
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}


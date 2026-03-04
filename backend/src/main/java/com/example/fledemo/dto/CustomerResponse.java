package com.example.fledemo.dto;

import com.example.fledemo.model.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private String tenantId;
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private String address;

    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .tenantId(customer.getTenantId())
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .build();
    }
}


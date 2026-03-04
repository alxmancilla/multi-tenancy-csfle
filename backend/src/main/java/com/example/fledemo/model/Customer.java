package com.example.fledemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String tenantId;
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
}


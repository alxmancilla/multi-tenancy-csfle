package com.example.fledemo.service;

import com.example.fledemo.keyvault.TenantKeyService;
import com.example.fledemo.model.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final TenantKeyService tenantKeyService;
    private final CustomerService customerService;
    private final OrderService orderService;

    public DataSeeder(TenantKeyService tenantKeyService, 
                     CustomerService customerService, 
                     OrderService orderService) {
        this.tenantKeyService = tenantKeyService;
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @Override
    public void run(String... args) {
        log.info("Starting data seeding...");
        
        Map<String, String[]> tenantCustomerIds = new HashMap<>();
        
        for (String tenantId : tenantKeyService.getAllTenantIds()) {
            String[] customerIds = seedTenantData(tenantId);
            tenantCustomerIds.put(tenantId, customerIds);
        }
        
        log.info("Data seeding completed successfully");
    }

    private String[] seedTenantData(String tenantId) {
        log.info("Seeding data for tenant: {}", tenantId);
        
        String[] customerIds = new String[2];
        
        if (tenantId.equals("tenant_alpha")) {
            Customer c1 = customerService.createCustomer(
                tenantId,
                "Alice Anderson",
                "alice@acmecorp.com",
                "+1-555-0101",
                "123 Main St, New York, NY 10001"
            );
            customerIds[0] = c1.getCustomerId();
            
            Customer c2 = customerService.createCustomer(
                tenantId,
                "Bob Brown",
                "bob@acmecorp.com",
                "+1-555-0102",
                "456 Oak Ave, Boston, MA 02101"
            );
            customerIds[1] = c2.getCustomerId();
            
            orderService.createOrder(tenantId, c1.getCustomerId(), "Enterprise Software License", 9999.99, "completed");
            orderService.createOrder(tenantId, c2.getCustomerId(), "Cloud Storage Plan", 299.99, "pending");
            
        } else if (tenantId.equals("tenant_beta")) {
            Customer c1 = customerService.createCustomer(
                tenantId,
                "Charlie Chen",
                "charlie@globexinc.com",
                "+1-555-0201",
                "789 Pine Rd, San Francisco, CA 94102"
            );
            customerIds[0] = c1.getCustomerId();
            
            Customer c2 = customerService.createCustomer(
                tenantId,
                "Diana Davis",
                "diana@globexinc.com",
                "+1-555-0202",
                "321 Elm St, Seattle, WA 98101"
            );
            customerIds[1] = c2.getCustomerId();
            
            orderService.createOrder(tenantId, c1.getCustomerId(), "API Gateway Subscription", 1499.99, "completed");
            orderService.createOrder(tenantId, c2.getCustomerId(), "Database Hosting", 799.99, "active");
            
        } else if (tenantId.equals("tenant_gamma")) {
            Customer c1 = customerService.createCustomer(
                tenantId,
                "Eve Evans",
                "eve@initechllc.com",
                "+1-555-0301",
                "555 Maple Dr, Austin, TX 78701"
            );
            customerIds[0] = c1.getCustomerId();
            
            Customer c2 = customerService.createCustomer(
                tenantId,
                "Frank Foster",
                "frank@initechllc.com",
                "+1-555-0302",
                "777 Birch Ln, Denver, CO 80201"
            );
            customerIds[1] = c2.getCustomerId();
            
            orderService.createOrder(tenantId, c1.getCustomerId(), "Security Audit Service", 4999.99, "in_progress");
            orderService.createOrder(tenantId, c2.getCustomerId(), "Consulting Package", 2499.99, "completed");
        }
        
        log.info("Seeded 2 customers and 2 orders for tenant: {}", tenantId);
        return customerIds;
    }
}


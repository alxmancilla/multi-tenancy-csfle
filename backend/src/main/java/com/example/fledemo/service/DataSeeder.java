package com.example.fledemo.service;

import com.example.fledemo.keyvault.TenantKeyService;
import com.example.fledemo.model.Customer;
import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Component
@Order(2) // Run after MongoIndexInitializer (Order 1)
public class DataSeeder implements CommandLineRunner {

    private final TenantKeyService tenantKeyService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final MongoClient plainMongoClient;

    @Value("${mongodb.database}")
    private String databaseName;

    public DataSeeder(TenantKeyService tenantKeyService,
                     CustomerService customerService,
                     OrderService orderService,
                     MongoClient plainMongoClient) {
        this.tenantKeyService = tenantKeyService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.plainMongoClient = plainMongoClient;
    }

    @Override
    public void run(String... args) {
        log.info("Starting data seeding...");

        for (String tenantId : tenantKeyService.getAllTenantIds()) {
            try {
                // Check if data already exists for this tenant (tenantId is plaintext, no decryption needed)
                long existingCustomers = plainMongoClient.getDatabase(databaseName)
                        .getCollection("customers")
                        .countDocuments(eq("tenantId", tenantId));
                if (existingCustomers > 0) {
                    log.info("Tenant {} already has {} customers, skipping seeding", tenantId, existingCustomers);
                    continue;
                }

                seedTenantData(tenantId);
            } catch (Exception e) {
                log.error("Failed to seed data for tenant {}: {}", tenantId, e.getMessage());
                throw new IllegalStateException("Data seeding failed. See logs above for details.", e);
            }
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


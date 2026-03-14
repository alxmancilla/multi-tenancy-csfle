package com.example.fledemo.service;

import com.example.fledemo.keyvault.TenantKeyService;
import com.example.fledemo.model.Customer;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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

        Map<String, String[]> tenantCustomerIds = new HashMap<>();

        for (String tenantId : tenantKeyService.getAllTenantIds()) {
            try {
                // Check if data already exists for this tenant
                long existingCustomers = customerService.getCustomersByTenant(tenantId).size();
                if (existingCustomers > 0) {
                    log.info("Tenant {} already has {} customers, skipping seeding", tenantId, existingCustomers);
                    continue;
                }

                String[] customerIds = seedTenantData(tenantId);
                tenantCustomerIds.put(tenantId, customerIds);
            } catch (MongoException e) {
                // Check if this is an HMAC validation failure (master key mismatch)
                if (e.getMessage() != null && e.getMessage().contains("HMAC validation failure")) {
                    log.warn("HMAC validation failure detected for tenant {}. This indicates master keys have changed.", tenantId);
                    log.warn("Clearing all encrypted data and key vault to start fresh...");

                    clearDatabaseAndKeyVault();

                    log.info("Database cleared. Restarting data seeding...");
                    // Retry seeding after clearing
                    String[] customerIds = seedTenantData(tenantId);
                    tenantCustomerIds.put(tenantId, customerIds);
                } else {
                    log.error("Failed to seed data for tenant {}: {}", tenantId, e.getMessage());
                    throw new IllegalStateException("Data seeding failed. See logs above for details.", e);
                }
            } catch (Exception e) {
                log.error("Failed to seed data for tenant {}: {}", tenantId, e.getMessage());
                throw new IllegalStateException("Data seeding failed. See logs above for details.", e);
            }
        }

        log.info("Data seeding completed successfully");
    }

    /**
     * Clears all collections in the database when HMAC validation fails.
     * This happens when master keys have changed but old encrypted data still exists.
     */
    private void clearDatabaseAndKeyVault() {
        try {
            MongoDatabase database = plainMongoClient.getDatabase(databaseName);

            log.info("Dropping database '{}' to clear old encrypted data...", databaseName);
            database.drop();
            log.info("Database dropped successfully. New data will be encrypted with current master keys.");

        } catch (Exception e) {
            log.error("Failed to clear database: {}", e.getMessage());
            throw new IllegalStateException("Could not clear database after HMAC validation failure", e);
        }
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


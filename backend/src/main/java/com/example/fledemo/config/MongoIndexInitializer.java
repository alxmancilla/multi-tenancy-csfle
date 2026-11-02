package com.example.fledemo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Initializes MongoDB indexes and server-side encryption validators for multi-tenant collections.
 *
 * Applies a $jsonSchema validator derived from the encryption schema files so the server
 * rejects documents whose sensitive fields are not encrypted (e.g. writes from a plain client).
 *
 * Creates indexes to optimize query performance for tenant-isolated data access.
 * Runs before DataSeeder to ensure indexes exist before data insertion.
 * 
 * Indexes created:
 * - customers: { tenantId: 1 }, { tenantId: 1, email: 1 }, { tenantId: 1, customerId: 1 }
 * - orders: { tenantId: 1 }, { tenantId: 1, customerId: 1 }, { tenantId: 1, orderId: 1 }
 * 
 * These indexes are critical for:
 * 1. Tenant isolation - Fast filtering by tenantId
 * 2. Lookup performance - Composite indexes for common access patterns
 * 3. Uniqueness enforcement - Prevent duplicate emails/IDs within a tenant
 */
@Slf4j
@Component
@Order(1) // Run before DataSeeder (which has default order)
public class MongoIndexInitializer implements CommandLineRunner {

    private final MongoClient plainMongoClient;
    private final TenantMongoClientFactory tenantMongoClientFactory;

    @Value("${mongodb.database}")
    private String databaseName;

    public MongoIndexInitializer(MongoClient plainMongoClient, TenantMongoClientFactory tenantMongoClientFactory) {
        this.plainMongoClient = plainMongoClient;
        this.tenantMongoClientFactory = tenantMongoClientFactory;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing MongoDB indexes for multi-tenant collections...");

        MongoDatabase database = plainMongoClient.getDatabase(databaseName);

        tenantMongoClientFactory.getEncryptionSchemas()
                .forEach((collectionName, schema) -> applyEncryptionValidator(database, collectionName, schema));

        createCustomerIndexes(database);
        createOrderIndexes(database);
        
        log.info("MongoDB indexes initialized successfully");
    }

    /**
     * Applies a server-side $jsonSchema validator requiring encrypted fields to be stored as
     * encrypted BinData. keyId is omitted because each tenant uses a different DEK.
     */
    private void applyEncryptionValidator(MongoDatabase database, String collectionName, BsonDocument schema) {
        schema.remove("encryptMetadata");
        BsonDocument validator = new BsonDocument("$jsonSchema", schema);

        boolean exists = database.listCollectionNames().into(new ArrayList<>()).contains(collectionName);
        if (exists) {
            database.runCommand(new Document("collMod", collectionName)
                    .append("validator", validator)
                    .append("validationLevel", "strict")
                    .append("validationAction", "error"));
        } else {
            database.createCollection(collectionName, new CreateCollectionOptions()
                    .validationOptions(new ValidationOptions()
                            .validator(validator)
                            .validationLevel(ValidationLevel.STRICT)
                            .validationAction(ValidationAction.ERROR)));
        }
        log.info("Applied encryption validator on '{}' collection", collectionName);
    }

    private void createCustomerIndexes(MongoDatabase database) {
        MongoCollection<Document> customers = database.getCollection("customers");
        
        log.info("Creating indexes for 'customers' collection...");
        
        // Index 1: tenantId (for tenant isolation queries)
        customers.createIndex(
            Indexes.ascending("tenantId"),
            new IndexOptions().name("idx_tenantId")
        );
        log.info("Created index: idx_tenantId on customers");
        
        // Index 2: tenantId + email (for email lookups within tenant)
        // Email is deterministically encrypted, so it's queryable
        customers.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("tenantId"),
                Indexes.ascending("email")
            ),
            new IndexOptions()
                .name("idx_tenantId_email")
                .unique(true) // Enforce unique email per tenant
        );
        log.info("Created index: idx_tenantId_email (unique) on customers");
        
        // Index 3: tenantId + customerId (for customer ID lookups)
        customers.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("tenantId"),
                Indexes.ascending("customerId")
            ),
            new IndexOptions()
                .name("idx_tenantId_customerId")
                .unique(true) // Enforce unique customerId per tenant
        );
        log.info("Created index: idx_tenantId_customerId (unique) on customers");
    }

    private void createOrderIndexes(MongoDatabase database) {
        MongoCollection<Document> orders = database.getCollection("orders");
        
        log.info("Creating indexes for 'orders' collection...");
        
        // Index 1: tenantId (for tenant isolation queries)
        orders.createIndex(
            Indexes.ascending("tenantId"),
            new IndexOptions().name("idx_tenantId")
        );
        log.info("Created index: idx_tenantId on orders");
        
        // Index 2: tenantId + customerId (for customer's orders lookup)
        orders.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("tenantId"),
                Indexes.ascending("customerId")
            ),
            new IndexOptions().name("idx_tenantId_customerId")
        );
        log.info("Created index: idx_tenantId_customerId on orders");
        
        // Index 3: tenantId + orderId (for order ID lookups)
        orders.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("tenantId"),
                Indexes.ascending("orderId")
            ),
            new IndexOptions()
                .name("idx_tenantId_orderId")
                .unique(true) // Enforce unique orderId per tenant
        );
        log.info("Created index: idx_tenantId_orderId (unique) on orders");
    }
}


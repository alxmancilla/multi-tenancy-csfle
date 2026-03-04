package com.example.fledemo.service;

import com.example.fledemo.config.TenantMongoClientFactory;
import com.example.fledemo.model.Customer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Slf4j
@Service
public class CustomerService {

    private final TenantMongoClientFactory clientFactory;

    @Value("${mongodb.database}")
    private String databaseName;

    public CustomerService(TenantMongoClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public Customer createCustomer(String tenantId, String name, String email, String phone, String address) {
        log.info("Creating customer for tenant: {}", tenantId);
        
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase database = client.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("customers");

        String customerId = UUID.randomUUID().toString();
        
        Document doc = new Document()
                .append("tenantId", tenantId)
                .append("customerId", customerId)
                .append("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", address);

        collection.insertOne(doc);
        log.info("Customer created with ID: {}", customerId);

        return Customer.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .build();
    }

    public List<Customer> getCustomersByTenant(String tenantId) {
        log.info("Fetching customers for tenant: {}", tenantId);
        
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase database = client.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("customers");

        List<Customer> customers = new ArrayList<>();
        collection.find(eq("tenantId", tenantId)).forEach(doc -> {
            customers.add(documentToCustomer(doc));
        });

        log.info("Found {} customers for tenant: {}", customers.size(), tenantId);
        return customers;
    }

    public Customer getCustomerByEmail(String tenantId, String email) {
        log.info("Searching for customer by email in tenant: {}", tenantId);
        
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase database = client.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("customers");

        Document doc = collection.find(and(
                eq("tenantId", tenantId),
                eq("email", email)
        )).first();

        if (doc == null) {
            log.info("No customer found with email: {}", email);
            return null;
        }

        return documentToCustomer(doc);
    }

    private Customer documentToCustomer(Document doc) {
        return Customer.builder()
                .tenantId(doc.getString("tenantId"))
                .customerId(doc.getString("customerId"))
                .name(doc.getString("name"))
                .email(doc.getString("email"))
                .phone(doc.getString("phone"))
                .address(doc.getString("address"))
                .build();
    }
}


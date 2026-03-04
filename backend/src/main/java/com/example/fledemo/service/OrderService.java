package com.example.fledemo.service;

import com.example.fledemo.config.TenantMongoClientFactory;
import com.example.fledemo.model.Order;
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
public class OrderService {

    private final TenantMongoClientFactory clientFactory;

    @Value("${mongodb.database}")
    private String databaseName;

    public OrderService(TenantMongoClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public Order createOrder(String tenantId, String customerId, String product, Double amount, String status) {
        log.info("Creating order for tenant: {}", tenantId);
        
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase database = client.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("orders");

        String orderId = UUID.randomUUID().toString();
        
        Document doc = new Document()
                .append("tenantId", tenantId)
                .append("orderId", orderId)
                .append("customerId", customerId)
                .append("product", product)
                .append("amount", amount)
                .append("status", status);

        collection.insertOne(doc);
        log.info("Order created with ID: {}", orderId);

        return Order.builder()
                .tenantId(tenantId)
                .orderId(orderId)
                .customerId(customerId)
                .product(product)
                .amount(amount)
                .status(status)
                .build();
    }

    public List<Order> getOrdersByTenant(String tenantId) {
        log.info("Fetching orders for tenant: {}", tenantId);
        
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase database = client.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("orders");

        List<Order> orders = new ArrayList<>();
        collection.find(eq("tenantId", tenantId)).forEach(doc -> {
            orders.add(documentToOrder(doc));
        });

        log.info("Found {} orders for tenant: {}", orders.size(), tenantId);
        return orders;
    }

    public List<Order> getOrdersByCustomer(String tenantId, String customerId) {
        log.info("Fetching orders for customer {} in tenant: {}", customerId, tenantId);
        
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase database = client.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection("orders");

        List<Order> orders = new ArrayList<>();
        collection.find(and(
                eq("tenantId", tenantId),
                eq("customerId", customerId)
        )).forEach(doc -> {
            orders.add(documentToOrder(doc));
        });

        log.info("Found {} orders for customer: {}", orders.size(), customerId);
        return orders;
    }

    private Order documentToOrder(Document doc) {
        return Order.builder()
                .tenantId(doc.getString("tenantId"))
                .orderId(doc.getString("orderId"))
                .customerId(doc.getString("customerId"))
                .product(doc.getString("product"))
                .amount(doc.getDouble("amount"))
                .status(doc.getString("status"))
                .build();
    }
}


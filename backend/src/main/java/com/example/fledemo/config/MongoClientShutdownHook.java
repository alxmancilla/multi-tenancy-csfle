package com.example.fledemo.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ensures all MongoDB clients are properly closed on application shutdown.
 * 
 * This prevents resource leaks and ensures graceful shutdown of connection pools.
 */
@Slf4j
@Component
public class MongoClientShutdownHook {

    private final TenantMongoClientFactory tenantMongoClientFactory;

    public MongoClientShutdownHook(TenantMongoClientFactory tenantMongoClientFactory) {
        this.tenantMongoClientFactory = tenantMongoClientFactory;
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Application shutdown detected. Closing all MongoDB clients...");
        tenantMongoClientFactory.closeAllClients();
    }
}


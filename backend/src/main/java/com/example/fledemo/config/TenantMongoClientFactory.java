package com.example.fledemo.config;

import com.example.fledemo.keyvault.TenantKeyService;
import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating per-tenant MongoDB clients with automatic encryption.
 *
 * Each tenant gets a dedicated MongoClient configured with AutoEncryptionSettings
 * that use that tenant's specific Data Encryption Key (DEK).
 *
 * Features:
 * - LRU cache to limit active clients in memory (prevents resource exhaustion)
 * - Automatic client eviction when cache is full
 * - Encryption schemas loaded from JSON configuration files
 * - Connection pooling per client (maxSize: 20, minSize: 2)
 *
 * The MongoDB driver automatically loads the Automatic Encryption Shared Library
 * (libmongocrypt) from standard system locations:
 * - macOS/Linux: /usr/local/lib/mongo_crypt_v1.{dylib|so}
 * - Windows: C:\Windows\System32\mongo_crypt_v1.dll
 *
 * Alternative: Set MONGOCRYPT_SHARED_LIB_PATH environment variable to specify
 * a custom location for the shared library.
 */
@Slf4j
@Component
public class TenantMongoClientFactory {

    private static final int MAX_CACHED_CLIENTS = 50; // Maximum number of active clients in memory

    private final LocalKmsProvider localKmsProvider;
    private final TenantKeyService tenantKeyService;
    private final Map<String, MongoClient> tenantClients;
    private final Map<String, BsonDocument> schemaCache = new HashMap<>();

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Value("${mongodb.keyvault.namespace}")
    private String keyVaultNamespace;

    @Value("${mongodb.database}")
    private String databaseName;

    public TenantMongoClientFactory(LocalKmsProvider localKmsProvider, TenantKeyService tenantKeyService) {
        this.localKmsProvider = localKmsProvider;
        this.tenantKeyService = tenantKeyService;

        // Initialize LRU cache with automatic eviction
        this.tenantClients = new LinkedHashMap<>(MAX_CACHED_CLIENTS, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, MongoClient> eldest) {
                if (size() > MAX_CACHED_CLIENTS) {
                    log.info("LRU cache full. Evicting client for tenant: {}", eldest.getKey());
                    // Close the evicted client to free resources
                    try {
                        eldest.getValue().close();
                    } catch (Exception e) {
                        log.warn("Error closing evicted client for tenant {}: {}", eldest.getKey(), e.getMessage());
                    }
                    return true;
                }
                return false;
            }
        };

        loadEncryptionSchemas();
    }

    /**
     * Loads encryption schemas from JSON files in the config directory.
     * Schemas are cached to avoid repeated file I/O.
     */
    private void loadEncryptionSchemas() {
        log.info("Loading encryption schemas from JSON configuration files...");

        try {
            schemaCache.put("customers", loadSchemaFromFile("config/encryption-schema-customers.json"));
            schemaCache.put("orders", loadSchemaFromFile("config/encryption-schema-orders.json"));

            log.info("Successfully loaded {} encryption schemas", schemaCache.size());
        } catch (Exception e) {
            log.error("Failed to load encryption schemas: {}", e.getMessage());
            throw new IllegalStateException("Could not load encryption schemas from config files", e);
        }
    }

    /**
     * Loads a schema from a JSON file in the classpath.
     */
    private BsonDocument loadSchemaFromFile(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return BsonDocument.parse(json);
    }

    public synchronized MongoClient getClientForTenant(String tenantId) {
        MongoClient client = tenantClients.computeIfAbsent(tenantId, this::createClientForTenant);

        // Log cache statistics periodically
        if (tenantClients.size() % 10 == 0) {
            log.info("Client cache statistics: {} active clients (max: {})",
                     tenantClients.size(), MAX_CACHED_CLIENTS);
        }

        return client;
    }

    /**
     * Returns the number of currently cached MongoClients.
     */
    public int getCachedClientCount() {
        return tenantClients.size();
    }

    /**
     * Closes all cached MongoClients. Should be called on application shutdown.
     */
    public synchronized void closeAllClients() {
        log.info("Closing all {} cached MongoClients...", tenantClients.size());

        for (Map.Entry<String, MongoClient> entry : tenantClients.entrySet()) {
            try {
                entry.getValue().close();
                log.debug("Closed client for tenant: {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Error closing client for tenant {}: {}", entry.getKey(), e.getMessage());
            }
        }

        tenantClients.clear();
        log.info("All MongoClients closed successfully");
    }

    private MongoClient createClientForTenant(String tenantId) {
        log.info("Creating encrypted MongoClient for tenant: {} with dedicated master key", tenantId);

        BsonBinary dataKeyId = tenantKeyService.getDataKeyId(tenantId);
        Map<String, BsonDocument> encryptedFieldsMap = buildEncryptedFieldsMap(dataKeyId);

        // Use tenant-specific KMS provider - this ensures the client can ONLY
        // decrypt DEKs that were encrypted with this tenant's master key
        AutoEncryptionSettings autoEncryptionSettings = AutoEncryptionSettings.builder()
                .keyVaultNamespace(keyVaultNamespace)
                .kmsProviders(localKmsProvider.getKmsProvidersForTenant(tenantId))
                .schemaMap(encryptedFieldsMap)
                .build();

        // Configure connection pool to prevent exhausting connections with many tenants
        // maxPoolSize: Maximum connections per tenant client (default: 100)
        // minPoolSize: Minimum idle connections to maintain (default: 0)
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToConnectionPoolSettings(builder ->
                    builder.maxSize(20)  // Limit connections per tenant
                           .minSize(2)   // Keep some connections warm
                           .maxWaitTime(10, TimeUnit.SECONDS)
                )
                .autoEncryptionSettings(autoEncryptionSettings)
                .build();

        MongoClient client = MongoClients.create(clientSettings);
        log.info("Created encrypted MongoClient for tenant: {} with cryptographic isolation", tenantId);

        return client;
    }

    /**
     * Builds the encrypted fields map for all collections.
     * Loads schemas from JSON files and injects the tenant-specific DEK ID.
     */
    private Map<String, BsonDocument> buildEncryptedFieldsMap(BsonBinary dataKeyId) {
        Map<String, BsonDocument> schemaMap = new HashMap<>();

        // Load schemas from cache and inject tenant-specific DEK ID
        for (Map.Entry<String, BsonDocument> entry : schemaCache.entrySet()) {
            String collectionName = entry.getKey();
            BsonDocument schema = entry.getValue().clone(); // Clone to avoid modifying cache

            // Inject the tenant-specific Data Encryption Key ID
            schema.getDocument("encryptMetadata")
                  .getArray("keyId")
                  .add(dataKeyId);

            // Add to schema map with full namespace (database.collection)
            String namespace = databaseName + "." + collectionName;
            schemaMap.put(namespace, schema);

            log.debug("Loaded encryption schema for collection: {}", namespace);
        }

        return schemaMap;
    }
}


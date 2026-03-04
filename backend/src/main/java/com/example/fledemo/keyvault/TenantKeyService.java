package com.example.fledemo.keyvault;

import com.example.fledemo.config.LocalKmsProvider;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TenantKeyService {

    private final MongoClient plainMongoClient;
    private final LocalKmsProvider localKmsProvider;

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Value("${mongodb.keyvault.namespace}")
    private String keyVaultNamespace;

    private final Map<String, BsonBinary> tenantDataKeys = new HashMap<>();
    
    private static final List<String> TENANT_IDS = Arrays.asList(
            "tenant_alpha",
            "tenant_beta",
            "tenant_gamma"
    );

    public TenantKeyService(MongoClient plainMongoClient, LocalKmsProvider localKmsProvider) {
        this.plainMongoClient = plainMongoClient;
        this.localKmsProvider = localKmsProvider;
    }

    @PostConstruct
    public void initializeTenantKeys() {
        log.info("Initializing Data Encryption Keys for all tenants with separate master keys...");

        // Create DEKs for each tenant using their specific master key
        for (String tenantId : TENANT_IDS) {
            BsonBinary dataKeyId = getOrCreateDataKeyForTenant(tenantId);
            tenantDataKeys.put(tenantId, dataKeyId);
            log.info("Tenant '{}' DEK ID: {}", tenantId, dataKeyId);
        }

        log.info("All tenant Data Encryption Keys initialized successfully with separate master keys");
    }

    private BsonBinary getOrCreateDataKeyForTenant(String tenantId) {
        // Create a ClientEncryption with this tenant's specific master key
        ClientEncryptionSettings encryptionSettings = ClientEncryptionSettings.builder()
                .keyVaultMongoClientSettings(MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(mongoUri))
                        .build())
                .keyVaultNamespace(keyVaultNamespace)
                .kmsProviders(localKmsProvider.getKmsProviderForKeyCreation(tenantId))
                .build();

        try (ClientEncryption clientEncryption = ClientEncryptions.create(encryptionSettings)) {
            String keyAltName = tenantId;

            BsonDocument existingKey = clientEncryption.getKeyByAltName(keyAltName);
            if (existingKey != null) {
                BsonBinary keyId = existingKey.getBinary("_id");
                log.info("Found existing DEK for tenant '{}': {}", tenantId, keyId);
                return keyId;
            }

            log.info("Creating new DEK for tenant '{}' with dedicated master key", tenantId);
            DataKeyOptions dataKeyOptions = new DataKeyOptions()
                    .keyAltNames(Arrays.asList(keyAltName));

            // Use standard "local" provider - the master key itself provides isolation
            BsonBinary dataKeyId = clientEncryption.createDataKey("local", dataKeyOptions);
            log.info("Created new DEK for tenant '{}': {}", tenantId, dataKeyId);

            return dataKeyId;
        }
    }

    public BsonBinary getDataKeyId(String tenantId) {
        BsonBinary keyId = tenantDataKeys.get(tenantId);
        if (keyId == null) {
            throw new IllegalArgumentException("No data key found for tenant: " + tenantId);
        }
        return keyId;
    }

    public Map<String, BsonBinary> getAllDataKeyIds() {
        return new HashMap<>(tenantDataKeys);
    }

    public List<String> getAllTenantIds() {
        return TENANT_IDS;
    }
}


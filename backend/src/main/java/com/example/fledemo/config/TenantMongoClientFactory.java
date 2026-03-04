package com.example.fledemo.config;

import com.example.fledemo.keyvault.TenantKeyService;
import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating per-tenant MongoDB clients with automatic encryption.
 *
 * Each tenant gets a dedicated MongoClient configured with AutoEncryptionSettings
 * that use that tenant's specific Data Encryption Key (DEK).
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

    private final LocalKmsProvider localKmsProvider;
    private final TenantKeyService tenantKeyService;
    private final Map<String, MongoClient> tenantClients = new HashMap<>();

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Value("${mongodb.keyvault.namespace}")
    private String keyVaultNamespace;

    public TenantMongoClientFactory(LocalKmsProvider localKmsProvider, TenantKeyService tenantKeyService) {
        this.localKmsProvider = localKmsProvider;
        this.tenantKeyService = tenantKeyService;
    }

    public synchronized MongoClient getClientForTenant(String tenantId) {
        return tenantClients.computeIfAbsent(tenantId, this::createClientForTenant);
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

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .autoEncryptionSettings(autoEncryptionSettings)
                .build();

        MongoClient client = MongoClients.create(clientSettings);
        log.info("Created encrypted MongoClient for tenant: {} with cryptographic isolation", tenantId);

        return client;
    }

    private Map<String, BsonDocument> buildEncryptedFieldsMap(BsonBinary dataKeyId) {
        Map<String, BsonDocument> schemaMap = new HashMap<>();

        schemaMap.put("fle_demo.customers", buildCustomersSchema(dataKeyId));
        schemaMap.put("fle_demo.orders", buildOrdersSchema(dataKeyId));

        return schemaMap;
    }

    private BsonDocument buildCustomersSchema(BsonBinary dataKeyId) {
        BsonDocument schema = BsonDocument.parse("""
            {
                "bsonType": "object",
                "encryptMetadata": {
                    "keyId": []
                },
                "properties": {
                    "name": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    },
                    "email": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic"
                        }
                    },
                    "phone": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    },
                    "address": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    }
                }
            }
            """);

        // Add the dataKeyId to the keyId array
        schema.getDocument("encryptMetadata")
              .getArray("keyId")
              .add(dataKeyId);

        return schema;
    }

    private BsonDocument buildOrdersSchema(BsonBinary dataKeyId) {
        BsonDocument schema = BsonDocument.parse("""
            {
                "bsonType": "object",
                "encryptMetadata": {
                    "keyId": []
                },
                "properties": {
                    "product": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    },
                    "amount": {
                        "encrypt": {
                            "bsonType": "double",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    },
                    "status": {
                        "encrypt": {
                            "bsonType": "string",
                            "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
                        }
                    }
                }
            }
            """);

        // Add the dataKeyId to the keyId array
        schema.getDocument("encryptMetadata")
              .getArray("keyId")
              .add(dataKeyId);

        return schema;
    }
}


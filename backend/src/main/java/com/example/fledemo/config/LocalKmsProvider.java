package com.example.fledemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides per-tenant local KMS (Key Management Service) master keys for encrypting
 * Data Encryption Keys (DEKs).
 *
 * This generates a separate 96-byte random master key for EACH tenant on first run
 * and persists them to disk. Each tenant's DEK is encrypted with their own master key,
 * ensuring true cryptographic isolation between tenants.
 *
 * IMPORTANT: This uses a local KMS for demo purposes only. In production, use a
 * cloud KMS provider like AWS KMS, Azure Key Vault, or GCP Cloud KMS with separate
 * keys or key namespaces per tenant.
 *
 * NOTE: The actual encryption/decryption operations are performed by the
 * MongoDB Automatic Encryption Shared Library (libmongocrypt), which must be
 * installed on the system. See INSTALL_CRYPT_SHARED_LIBRARY.md for details.
 */
@Slf4j
@Component
public class LocalKmsProvider {

    @Value("${mongodb.local-master-key-path}")
    private String masterKeyBasePath;

    private final Map<String, byte[]> tenantMasterKeys = new HashMap<>();

    private static final List<String> TENANT_IDS = Arrays.asList(
            "tenant_alpha",
            "tenant_beta",
            "tenant_gamma"
    );

    @PostConstruct
    public void init() throws IOException {
        log.info("Initializing per-tenant master keys...");

        for (String tenantId : TENANT_IDS) {
            byte[] masterKey = loadOrGenerateMasterKey(tenantId);
            tenantMasterKeys.put(tenantId, masterKey);
            log.info("Master key initialized for tenant: {}", tenantId);
        }

        log.info("All tenant master keys initialized successfully");
    }

    private byte[] loadOrGenerateMasterKey(String tenantId) throws IOException {
        // Create a separate file for each tenant's master key
        // Replace the file extension (either .bin or .key) with _tenantId.bin
        String keyFileName;
        if (masterKeyBasePath.endsWith(".bin")) {
            keyFileName = masterKeyBasePath.replace(".bin", "_" + tenantId + ".bin");
        } else if (masterKeyBasePath.endsWith(".key")) {
            keyFileName = masterKeyBasePath.replace(".key", "_" + tenantId + ".key");
        } else {
            keyFileName = masterKeyBasePath + "_" + tenantId;
        }

        Path keyPath = Paths.get(keyFileName);

        // Check if path exists and is a directory (Docker bind mount issue)
        if (Files.exists(keyPath) && Files.isDirectory(keyPath)) {
            throw new IllegalStateException(
                "Master key path exists but is a directory: " + keyFileName + ". " +
                "This usually happens when Docker creates a directory for a non-existent bind mount. " +
                "Use a named volume instead of bind mounting individual files."
            );
        }

        if (Files.exists(keyPath)) {
            log.info("Loading existing master key for tenant '{}' from: {}", tenantId, keyFileName);
            try (FileInputStream fis = new FileInputStream(keyPath.toFile())) {
                byte[] masterKey = fis.readAllBytes();
                if (masterKey.length != 96) {
                    throw new IllegalStateException("Master key for tenant '" + tenantId + "' must be exactly 96 bytes");
                }
                return masterKey;
            }
        } else {
            log.info("Generating new master key for tenant '{}' and saving to: {}", tenantId, keyFileName);
            byte[] masterKey = new byte[96];
            new SecureRandom().nextBytes(masterKey);

            // Safely create parent directories if they exist and are not null
            Path parentPath = keyPath.getParent();
            if (parentPath != null && !Files.exists(parentPath)) {
                log.info("Creating parent directory: {}", parentPath);
                Files.createDirectories(parentPath);
            }

            try (FileOutputStream fos = new FileOutputStream(keyPath.toFile())) {
                fos.write(masterKey);
            }
            log.info("Master key for tenant '{}' generated and saved successfully", tenantId);
            return masterKey;
        }
    }

    /**
     * Get KMS providers for a specific tenant.
     * Each tenant gets their own KMS provider with their own master key.
     */
    public Map<String, Map<String, Object>> getKmsProvidersForTenant(String tenantId) {
        byte[] masterKey = tenantMasterKeys.get(tenantId);
        if (masterKey == null) {
            throw new IllegalArgumentException("No master key found for tenant: " + tenantId);
        }

        Map<String, Object> localConfig = new HashMap<>();
        localConfig.put("key", masterKey);

        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        kmsProviders.put("local", localConfig);

        return kmsProviders;
    }

    /**
     * Get KMS provider for a specific tenant (for ClientEncryption during key creation).
     * This is used when creating DEKs - each tenant's DEK is created with their specific master key.
     */
    public Map<String, Map<String, Object>> getKmsProviderForKeyCreation(String tenantId) {
        byte[] masterKey = tenantMasterKeys.get(tenantId);
        if (masterKey == null) {
            throw new IllegalArgumentException("No master key found for tenant: " + tenantId);
        }

        Map<String, Object> localConfig = new HashMap<>();
        localConfig.put("key", masterKey);

        Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
        // Use standard "local" provider name - the master key itself provides the isolation
        kmsProviders.put("local", localConfig);

        return kmsProviders;
    }

    public byte[] getMasterKeyForTenant(String tenantId) {
        byte[] masterKey = tenantMasterKeys.get(tenantId);
        if (masterKey == null) {
            throw new IllegalArgumentException("No master key found for tenant: " + tenantId);
        }
        return masterKey;
    }
}


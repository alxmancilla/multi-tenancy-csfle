# Separate Master Keys Per Tenant - Implementation Guide

## 🎯 Overview

This application now implements **true cryptographic isolation** for multi-tenant CSFLE by using **separate master keys per tenant**. This ensures that cross-tenant decryption is cryptographically impossible, even if an attacker gains access to another tenant's encrypted data.

## 🔒 Security Architecture

### Before: Shared Master Key (Vulnerable)
```
┌─────────────────────────────────────────────────────────────┐
│                    Single Master Key                         │
│                    (96-byte local KMS)                       │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
   DEK_alpha           DEK_beta           DEK_gamma
        │                   │                   │
        ▼                   ▼                   ▼
  Tenant Alpha        Tenant Beta        Tenant Gamma
   Documents           Documents           Documents
```

**Problem**: Any tenant client with access to the shared master key could decrypt ANY DEK in the key vault, allowing cross-tenant decryption.

### After: Separate Master Keys (Secure)
```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Master Key   │    │ Master Key   │    │ Master Key   │
│   Alpha      │    │   Beta       │    │   Gamma      │
│ (96 bytes)   │    │ (96 bytes)   │    │ (96 bytes)   │
└──────────────┘    └──────────────┘    └──────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
   DEK_alpha           DEK_beta           DEK_gamma
       │                   │                   │
       ▼                   ▼                   ▼
  Tenant Alpha        Tenant Beta        Tenant Gamma
   Documents           Documents           Documents
```

**Solution**: Each tenant has their own master key. Without the victim's master key, an attacker cannot decrypt the victim's DEK, making cross-tenant decryption cryptographically impossible.

## 📁 Implementation Details

### 1. LocalKmsProvider Changes

**File**: `backend/src/main/java/com/example/fledemo/config/LocalKmsProvider.java`

**Key Changes**:
- Generates and stores **separate master key files** for each tenant:
  - `local_master_key_tenant_alpha.bin`
  - `local_master_key_tenant_beta.bin`
  - `local_master_key_tenant_gamma.bin`
- Provides `getKmsProvidersForTenant(tenantId)` - returns only that tenant's master key
- Provides `getAllKmsProviders()` - returns all master keys (for initialization only)

**Master Key Storage**:
```java
private final Map<String, byte[]> tenantMasterKeys = new HashMap<>();
```

### 2. TenantKeyService Changes

**File**: `backend/src/main/java/com/example/fledemo/keyvault/TenantKeyService.java`

**Key Changes**:
- Creates a separate `ClientEncryption` for each tenant during DEK creation
- Each `ClientEncryption` is configured with that tenant's specific master key
- All DEKs use the standard `"local"` provider name, but are encrypted with different master keys
- The master key itself provides the cryptographic isolation

**DEK Creation**:
```java
// Create ClientEncryption with tenant-specific master key
ClientEncryptionSettings encryptionSettings = ClientEncryptionSettings.builder()
    .kmsProviders(localKmsProvider.getKmsProviderForKeyCreation(tenantId))
    .build();

// Create DEK with standard "local" provider - the master key provides isolation
BsonBinary dataKeyId = clientEncryption.createDataKey("local", dataKeyOptions);
```

### 3. TenantMongoClientFactory Changes

**File**: `backend/src/main/java/com/example/fledemo/config/TenantMongoClientFactory.java`

**Key Changes**:
- Each tenant's MongoClient is configured with **only their own master key**
- Uses `getKmsProvidersForTenant(tenantId)` instead of a shared KMS provider
- This ensures the client can ONLY decrypt DEKs encrypted with that tenant's master key

**Client Configuration**:
```java
AutoEncryptionSettings autoEncryptionSettings = AutoEncryptionSettings.builder()
    .keyVaultNamespace(keyVaultNamespace)
    .kmsProviders(localKmsProvider.getKmsProvidersForTenant(tenantId))  // Tenant-specific!
    .schemaMap(encryptedFieldsMap)
    .build();
```

## 🧪 Testing the Implementation

### Expected Behavior

1. **Same-Tenant Access** ✅
   - Tenant Alpha can decrypt Tenant Alpha's data
   - Result: "ℹ️ EXPECTED: Same tenant can decrypt their own data"

2. **Cross-Tenant Attack** 🛡️
   - Tenant Alpha attempts to decrypt Tenant Beta's data
   - Result: "✅ SUCCESS: Cryptographic Isolation Enforced!"
   - Fields remain as Binary (encrypted) data
   - MongoDB cannot decrypt because it doesn't have Tenant Beta's master key

### How to Test

1. **Clean up old master keys** (if you had the old implementation):
   ```bash
   cd backend
   rm -f local_master_key*.bin
   ```

2. **Rebuild and restart the backend**:
   ```bash
   mvn clean install -DskipTests
   mvn spring-boot:run
   ```

3. **Check the logs** - you should see:
   ```
   Initializing per-tenant master keys...
   Generating new master key for tenant 'tenant_alpha' and saving to: ./local_master_key_tenant_alpha.bin
   Generating new master key for tenant 'tenant_beta' and saving to: ./local_master_key_tenant_beta.bin
   Generating new master key for tenant 'tenant_gamma' and saving to: ./local_master_key_tenant_gamma.bin
   All tenant master keys initialized successfully
   ```

4. **Run the Cross-Tenant Attack Simulation** in the UI:
   - Select different tenants (e.g., tenant_alpha attacking tenant_beta)
   - Click "Simulate Attack"
   - You should see: "✅ SUCCESS: Cryptographic Isolation Enforced!"

## 🔑 Key Vault Structure

The `__keyVault` collection now contains DEKs encrypted with different master keys:

```javascript
{
  "_id": UUID("..."),
  "keyAltNames": ["tenant_alpha"],
  "keyMaterial": Binary(...),  // Encrypted with Master Key Alpha
  "masterKey": {
    "provider": "local"  // All use "local" provider, but different master keys
  }
}
{
  "_id": UUID("..."),
  "keyAltNames": ["tenant_beta"],
  "keyMaterial": Binary(...),  // Encrypted with Master Key Beta (different from Alpha)
  "masterKey": {
    "provider": "local"  // Same provider name, different master key
  }
}
```

**Key Point**: All DEKs use the same provider name (`"local"`), but each is encrypted with a different master key. When a tenant's client tries to decrypt a DEK:
1. It retrieves the DEK from the key vault
2. It attempts to decrypt the DEK's `keyMaterial` using its master key
3. If the DEK was encrypted with a different master key, decryption fails
4. This prevents cross-tenant data access

## 🚀 Production Recommendations

For production environments, replace the local KMS with a cloud KMS provider:

### AWS KMS
```java
Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
kmsProviders.put("aws", Map.of(
    "accessKeyId", "...",
    "secretAccessKey", "...",
    "region", "us-east-1"
));
```

### Azure Key Vault
```java
Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
kmsProviders.put("azure", Map.of(
    "tenantId", "...",
    "clientId", "...",
    "clientSecret", "..."
));
```

### GCP Cloud KMS
```java
Map<String, Map<String, Object>> kmsProviders = new HashMap<>();
kmsProviders.put("gcp", Map.of(
    "email", "...",
    "privateKey", "..."
));
```

**Best Practice**: Use separate KMS keys or key namespaces per tenant in your cloud KMS provider.

## 📚 Additional Resources

- [MongoDB CSFLE Documentation](https://www.mongodb.com/docs/manual/core/csfle/)
- [MongoDB KMS Providers](https://www.mongodb.com/docs/manual/core/csfle/reference/kms-providers/)
- [Multi-Tenancy Best Practices](https://www.mongodb.com/docs/manual/core/csfle/fundamentals/manage-keys/)


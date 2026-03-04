# Architecture Documentation

## Overview

This application demonstrates MongoDB Client-Side Field Level Encryption (CSFLE) in a multi-tenant SaaS scenario where multiple tenants share the same database and collections, but data is cryptographically isolated using per-tenant Data Encryption Keys (DEKs).

**Key Technology:** This demo uses the **MongoDB Automatic Encryption Shared Library** (the modern replacement for `mongocryptd`) to perform client-side encryption and decryption operations.

## Encryption Flow

### 1. Master Key (Local KMS)
```
LocalKmsProvider
├── Generates 96-byte random key on first run
├── Persists to local_master_key.bin
└── Loads from disk on subsequent runs
```

### 2. Data Encryption Keys (DEKs)
```
TenantKeyService (on startup)
├── For each tenant (alpha, beta, gamma):
│   ├── Check if DEK exists in __keyVault
│   ├── If not, create new DEK using ClientEncryption.createDataKey()
│   ├── DEK is encrypted with Master Key
│   ├── Store DEK in __keyVault with keyAltName = tenantId
│   └── Cache DEK UUID in memory
```

### 3. Per-Tenant MongoClient Creation
```
TenantMongoClientFactory.getClientForTenant(tenantId)
├── Retrieve tenant's DEK UUID from TenantKeyService
├── Build encryptedFieldsMap with DEK for customers & orders collections
├── Create AutoEncryptionSettings with:
│   ├── keyVaultNamespace: "fle_demo.__keyVault"
│   ├── kmsProviders: Local KMS configuration
│   └── schemaMap: Encryption rules for this tenant
├── Create MongoClient with AutoEncryptionSettings
└── Cache client for reuse
```

### 4. Encryption Schema

**Customers Collection:**
```json
{
  "bsonType": "object",
  "encryptMetadata": {
    "keyId": [<tenant-specific-DEK-UUID>]
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
    "phone": { "encrypt": { "algorithm": "Random" } },
    "address": { "encrypt": { "algorithm": "Random" } }
  }
}
```

**Orders Collection:**
```json
{
  "bsonType": "object",
  "encryptMetadata": {
    "keyId": [<tenant-specific-DEK-UUID>]
  },
  "properties": {
    "product": { "encrypt": { "algorithm": "Random" } },
    "amount": { "encrypt": { "bsonType": "double", "algorithm": "Random" } },
    "status": { "encrypt": { "algorithm": "Random" } }
  }
}
```

## Data Flow

### Write Operation (Create Customer)
```
1. Frontend: POST /api/v1/tenants/tenant_alpha/customers
   └── Body: { name, email, phone, address }

2. CustomerController receives request
   └── Calls CustomerService.createCustomer(tenantId, ...)

3. CustomerService
   ├── Gets encrypted MongoClient for tenant_alpha
   ├── Creates Document with plaintext + sensitive fields
   └── Calls collection.insertOne(doc)

4. MongoDB Driver (with AutoEncryptionSettings)
   ├── Detects fields marked for encryption in schema
   ├── Calls Automatic Encryption Shared Library (libmongocrypt)
   ├── Encrypts name, email, phone, address using tenant_alpha's DEK
   ├── email → deterministic encryption (same input = same ciphertext)
   ├── name, phone, address → randomized encryption (same input = different ciphertext)
   └── Sends encrypted document to MongoDB

5. MongoDB Server
   └── Stores document with encrypted fields as BinData
```

### Read Operation (Get Customers)
```
1. Frontend: GET /api/v1/tenants/tenant_alpha/customers

2. CustomerController → CustomerService.getCustomersByTenant(tenantId)

3. CustomerService
   ├── Gets encrypted MongoClient for tenant_alpha
   └── Calls collection.find(eq("tenantId", "tenant_alpha"))

4. MongoDB Server
   └── Returns documents with encrypted BinData fields

5. MongoDB Driver (with AutoEncryptionSettings)
   ├── Detects encrypted fields
   ├── Retrieves tenant_alpha's DEK from __keyVault
   ├── Calls Automatic Encryption Shared Library to decrypt
   ├── Decrypts fields using the DEK
   └── Returns plaintext document to application

6. CustomerService
   └── Converts Document to Customer object

7. CustomerController
   └── Returns CustomerResponse to frontend
```

### Deterministic Encryption Query (Search by Email)
```
1. Frontend: GET /api/v1/tenants/tenant_alpha/customers/search?email=alice@acmecorp.com

2. CustomerService.getCustomerByEmail(tenantId, email)

3. MongoDB Driver
   ├── Detects email field is deterministically encrypted
   ├── Encrypts search value "alice@acmecorp.com" using tenant_alpha's DEK
   └── Sends query with encrypted email value

4. MongoDB Server
   ├── Performs equality match on encrypted field
   └── Returns matching document (if exists)

5. MongoDB Driver
   └── Decrypts returned document

Note: This ONLY works because email uses deterministic encryption.
Randomized fields cannot be queried (different ciphertext each time).
```

## Security Architecture

### Cryptographic Isolation

**Scenario: Tenant Beta tries to read Tenant Alpha's data**

```
1. Attacker uses plain MongoClient to retrieve raw document
   └── Gets document with encrypted BinData fields

2. Attacker uses Tenant Beta's encrypted MongoClient
   └── Attempts to decrypt the document

3. MongoDB Driver
   ├── Retrieves Tenant Beta's DEK from __keyVault
   ├── Attempts to decrypt fields encrypted with Tenant Alpha's DEK
   └── FAILS: Wrong key cannot decrypt the data

Result: Decryption fails or returns garbled data
```

### Key Hierarchy

```
Master Key (96 bytes, Local KMS)
├── Encrypts DEK for tenant_alpha
├── Encrypts DEK for tenant_beta
└── Encrypts DEK for tenant_gamma

Each DEK (stored in __keyVault)
├── Encrypted with Master Key
├── Identified by keyAltName (tenant ID)
└── Used to encrypt/decrypt that tenant's data
```

### Why This is Secure

1. **Client-Side Encryption**: MongoDB server never sees plaintext
2. **Per-Tenant Keys**: Each tenant has unique DEK
3. **Key Isolation**: DEKs are cryptographically separated
4. **Master Key Protection**: DEKs are encrypted at rest
5. **No Shared Secrets**: Tenants cannot access each other's keys

## Component Responsibilities

### Backend Components

| Component | Responsibility |
|-----------|---------------|
| `LocalKmsProvider` | Master key generation and management |
| `TenantKeyService` | DEK creation and caching |
| `TenantMongoClientFactory` | Per-tenant encrypted client creation |
| `MongoConfig` | Plain client for key vault and raw access |
| `CustomerService` | Business logic with encrypted operations |
| `OrderService` | Business logic with encrypted operations |
| `DataSeeder` | Initial data population |
| `DemoController` | Raw data view and attack simulation |

### Frontend Components

| Component | Responsibility |
|-----------|---------------|
| `TenantSelector` | Tenant context switching |
| `CustomersPanel` | Customer CRUD and search |
| `OrdersPanel` | Order CRUD |
| `DbaViewPanel` | Raw encrypted data visualization |
| `CrossTenantPanel` | Attack simulation UI |

## Database Schema

### __keyVault Collection
```json
{
  "_id": UUID("..."),
  "keyAltNames": ["tenant_alpha"],
  "keyMaterial": BinData(...),  // Encrypted with Master Key
  "creationDate": ISODate(...),
  "updateDate": ISODate(...),
  "status": 1,
  "masterKey": {
    "provider": "local"
  }
}
```

### customers Collection (as stored)
```json
{
  "_id": ObjectId("..."),
  "tenantId": "tenant_alpha",           // Plaintext
  "customerId": "uuid-...",             // Plaintext
  "name": BinData(6, "..."),           // Encrypted (Random)
  "email": BinData(6, "..."),          // Encrypted (Deterministic)
  "phone": BinData(6, "..."),          // Encrypted (Random)
  "address": BinData(6, "...")         // Encrypted (Random)
}
```

### orders Collection (as stored)
```json
{
  "_id": ObjectId("..."),
  "tenantId": "tenant_alpha",           // Plaintext
  "orderId": "uuid-...",                // Plaintext
  "customerId": "uuid-...",             // Plaintext
  "product": BinData(6, "..."),        // Encrypted (Random)
  "amount": BinData(6, "..."),         // Encrypted (Random)
  "status": BinData(6, "...")          // Encrypted (Random)
}
```

## Performance Considerations

1. **Client Caching**: MongoClients are cached per tenant (avoid recreation)
2. **DEK Caching**: DEK UUIDs cached in memory (avoid key vault lookups)
3. **Encryption Overhead**: ~10-20% performance impact for encryption/decryption
4. **Query Limitations**: Only deterministic fields support equality queries
5. **Index Strategy**: Index plaintext fields (tenantId, customerId, orderId)

## Production Recommendations

1. **Use Cloud KMS**: Replace Local KMS with AWS KMS, Azure Key Vault, or GCP KMS
2. **Key Rotation**: Implement periodic DEK rotation
3. **Audit Logging**: Log all key access and encryption operations
4. **TLS**: Enable TLS for MongoDB connections
5. **Authentication**: Add proper user authentication and authorization
6. **Separate Databases**: Consider separate databases per tenant for additional isolation
7. **Monitoring**: Monitor encryption/decryption performance
8. **Backup Strategy**: Ensure master key is backed up securely


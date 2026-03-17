# Bypass Auto-Encryption Guide

## Overview

MongoDB Client-Side Field Level Encryption (CSFLE) adds encryption/decryption overhead to every operation. For operations that don't involve encrypted fields, you can bypass auto-encryption to improve performance.

## When to Use Each Client Type

### ✅ Use ENCRYPTED Client (Default)

**Required for operations involving sensitive data:**
- Creating/updating customers or orders
- Reading customer/order details
- Querying by encrypted fields (e.g., email search)
- Any operation that reads or writes encrypted fields

**Example:**
```java
// Use encrypted client for customer operations
MongoClient encryptedClient = tenantMongoClientFactory.getClientForTenant(tenantId);
MongoDatabase database = encryptedClient.getDatabase("fle_demo");
MongoCollection<Document> customers = database.getCollection("customers");

// This will automatically decrypt the encrypted fields
Document customer = customers.find(new Document("tenantId", tenantId)).first();
```

### ✅ Use PLAIN Client (Bypass Encryption)

**Suitable for operations that don't involve encrypted fields:**
- Viewing raw encrypted documents (DBA view)
- Creating indexes
- Database administration
- Metadata queries (count, distinct on non-encrypted fields)
- Querying only non-encrypted fields (tenantId, customerId, _id)
- Collection management

**Example:**
```java
// Use plain client for raw document viewing
MongoClient plainClient = ... // Injected plain client
MongoDatabase database = plainClient.getDatabase("fle_demo");
MongoCollection<Document> customers = database.getCollection("customers");

// This will show encrypted fields as Binary data
Document rawCustomer = customers.find(new Document("tenantId", tenantId)).first();
// Output: { name: Binary(...), email: Binary(...), ... }
```

## Performance Impact

### Encrypted Client Overhead
- **Encryption:** ~1-5ms per document write
- **Decryption:** ~1-5ms per document read
- **Key fetching:** Additional latency on first use
- **Memory:** Encryption state machine overhead

### Plain Client Benefits
- **No encryption overhead:** Direct MongoDB operations
- **Faster queries:** No decryption processing
- **Lower memory:** No encryption state
- **Better for bulk operations:** Significant savings on large datasets

## Use Cases

### 1. DBA View / Raw Documents
```java
// ✅ CORRECT: Use plain client
MongoClient plainClient = ...;
MongoCollection<Document> collection = plainClient.getDatabase("fle_demo")
    .getCollection("customers");
List<Document> rawDocs = collection.find().into(new ArrayList<>());
// Shows encrypted fields as Binary
```

### 2. Index Creation
```java
// ✅ CORRECT: Use plain client
MongoClient plainClient = ...;
MongoCollection<Document> collection = plainClient.getDatabase("fle_demo")
    .getCollection("customers");
collection.createIndex(Indexes.ascending("tenantId"));
// No encryption needed for index creation
```

### 3. Metadata Queries
```java
// ✅ CORRECT: Use plain client for counting
MongoClient plainClient = ...;
long count = plainClient.getDatabase("fle_demo")
    .getCollection("customers")
    .countDocuments(new Document("tenantId", tenantId));
// Only queries non-encrypted field
```

### 4. Customer CRUD Operations
```java
// ✅ CORRECT: Use encrypted client
MongoClient encryptedClient = tenantMongoClientFactory.getClientForTenant(tenantId);
MongoCollection<Document> collection = encryptedClient.getDatabase("fle_demo")
    .getCollection("customers");

Document customer = new Document()
    .append("tenantId", tenantId)
    .append("name", "Alice")  // Will be encrypted
    .append("email", "alice@example.com");  // Will be encrypted
collection.insertOne(customer);
```

## Important Notes

### ⚠️ Security Considerations

1. **Never use plain client to read sensitive data** - You'll get encrypted binary data, not plaintext
2. **Always filter by tenantId** - Even with plain client, implement application-level access control
3. **Audit plain client usage** - Document why bypass is used for each operation

### 📊 Performance Guidelines

1. **Bulk operations** - Consider plain client for operations on non-encrypted fields only
2. **Read-heavy workloads** - Encrypted client caches keys, so overhead decreases over time
3. **Write-heavy workloads** - Encryption overhead is more significant
4. **Mixed workloads** - Use encrypted client by default, plain client for specific optimizations

## Code Examples

### Example 1: Tenant Statistics (Plain Client)
```java
@Service
public class TenantStatisticsService {
    private final MongoClient plainMongoClient;
    
    public Map<String, Long> getTenantCounts() {
        MongoDatabase db = plainMongoClient.getDatabase("fle_demo");
        Map<String, Long> counts = new HashMap<>();
        
        // Count customers per tenant (no encryption needed)
        for (String tenantId : getAllTenantIds()) {
            long count = db.getCollection("customers")
                .countDocuments(new Document("tenantId", tenantId));
            counts.put(tenantId, count);
        }
        
        return counts;
    }
}
```

### Example 2: Customer Service (Encrypted Client)
```java
@Service
public class CustomerService {
    private final TenantMongoClientFactory clientFactory;
    
    public Customer getCustomer(String tenantId, String customerId) {
        // Use encrypted client to decrypt customer data
        MongoClient client = clientFactory.getClientForTenant(tenantId);
        MongoDatabase db = client.getDatabase("fle_demo");
        
        Document doc = db.getCollection("customers")
            .find(new Document("tenantId", tenantId)
                .append("customerId", customerId))
            .first();
        
        // Fields are automatically decrypted
        return mapToCustomer(doc);
    }
}
```

## Summary

| Operation Type | Client Type | Reason |
|----------------|-------------|--------|
| Read/Write encrypted fields | ENCRYPTED | Required for decryption |
| Raw document viewing | PLAIN | Show encrypted data as-is |
| Index creation | PLAIN | No encryption needed |
| Metadata queries | PLAIN | Performance optimization |
| Admin operations | PLAIN | No sensitive data involved |
| Customer/Order CRUD | ENCRYPTED | Involves sensitive fields |

**Default Rule:** When in doubt, use the **ENCRYPTED** client. Only use PLAIN client when you're certain the operation doesn't involve encrypted fields.


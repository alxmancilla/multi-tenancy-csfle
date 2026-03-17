# MongoDB CSFLE Encryption Schema Configuration

This directory contains JSON schema files that define which fields should be encrypted in each collection.

## Schema Files

- `encryption-schema-customers.json` - Encryption schema for the `customers` collection
- `encryption-schema-orders.json` - Encryption schema for the `orders` collection

## Schema Format

Each schema file follows the MongoDB JSON Schema format with encryption metadata:

```json
{
  "bsonType": "object",
  "encryptMetadata": {
    "keyId": []
  },
  "properties": {
    "fieldName": {
      "encrypt": {
        "bsonType": "string|double|int|...",
        "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random|Deterministic"
      }
    }
  }
}
```

## Encryption Algorithms

### AEAD_AES_256_CBC_HMAC_SHA_512-Random
- **Use for:** Sensitive data that doesn't need to be queried
- **Examples:** Names, phone numbers, addresses, amounts
- **Characteristics:**
  - Different ciphertext for same plaintext (non-deterministic)
  - Cannot be used in queries (WHERE, GROUP BY, etc.)
  - Highest security - prevents frequency analysis

### AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic
- **Use for:** Data that needs to be queried
- **Examples:** Email addresses, user IDs, account numbers
- **Characteristics:**
  - Same ciphertext for same plaintext (deterministic)
  - Can be used in equality queries (WHERE email = '...')
  - Cannot be used in range queries (>, <, BETWEEN)
  - Slightly lower security - allows frequency analysis

## Modifying Schemas

To add or modify encrypted fields:

1. Edit the appropriate JSON file
2. Add/modify the field definition under `properties`
3. Choose the appropriate encryption algorithm
4. Restart the application - no code changes needed!

## Example: Adding a New Encrypted Field

To encrypt a new `ssn` field in customers:

```json
{
  "properties": {
    "ssn": {
      "encrypt": {
        "bsonType": "string",
        "algorithm": "AEAD_AES_256_CBC_HMAC_SHA_512-Random"
      }
    }
  }
}
```

## Important Notes

1. **keyId array** - Left empty in the schema files. The application automatically injects the tenant-specific Data Encryption Key (DEK) ID at runtime.

2. **Collection naming** - Schema files are loaded based on collection names:
   - `encryption-schema-customers.json` → `customers` collection
   - `encryption-schema-orders.json` → `orders` collection

3. **Database prefix** - The application automatically prefixes collection names with the database name (e.g., `fle_demo.customers`).

4. **Queryable fields** - Only fields encrypted with `Deterministic` algorithm can be used in queries. Plan your schema accordingly based on access patterns.

## Security Best Practices

1. **Minimize deterministic encryption** - Use only when absolutely necessary for queries
2. **Encrypt sensitive data** - Always encrypt PII (Personally Identifiable Information)
3. **Document decisions** - Comment why each field uses a specific algorithm
4. **Review regularly** - Audit encryption schemas as requirements change


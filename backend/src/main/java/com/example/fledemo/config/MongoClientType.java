package com.example.fledemo.config;

/**
 * Enum to specify which type of MongoDB client to use for an operation.
 * 
 * This allows selective bypassing of auto-encryption for operations that don't
 * involve sensitive data, improving performance.
 */
public enum MongoClientType {
    /**
     * Encrypted client - Use for operations involving sensitive fields.
     * Automatically encrypts writes and decrypts reads.
     * Required for: Customer/Order CRUD operations.
     */
    ENCRYPTED,
    
    /**
     * Plain client - Use for operations that don't involve encrypted fields.
     * Bypasses auto-encryption overhead.
     * Suitable for: Metadata queries, index creation, raw document viewing, admin operations.
     */
    PLAIN
}


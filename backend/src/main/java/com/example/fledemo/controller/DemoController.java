package com.example.fledemo.controller;

import com.example.fledemo.config.TenantMongoClientFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final MongoClient plainMongoClient;
    private final TenantMongoClientFactory clientFactory;

    @Value("${mongodb.database}")
    private String databaseName;

    public DemoController(MongoClient plainMongoClient, TenantMongoClientFactory clientFactory) {
        this.plainMongoClient = plainMongoClient;
        this.clientFactory = clientFactory;
    }

    @GetMapping("/raw-documents")
    public ResponseEntity<Map<String, Object>> getRawDocuments() {
        log.info("Fetching raw (encrypted) documents from database");
        
        MongoDatabase database = plainMongoClient.getDatabase(databaseName);
        
        List<Document> rawCustomers = new ArrayList<>();
        MongoCollection<Document> customersCollection = database.getCollection("customers");
        customersCollection.find().limit(5).forEach(rawCustomers::add);
        
        List<Document> rawOrders = new ArrayList<>();
        MongoCollection<Document> ordersCollection = database.getCollection("orders");
        ordersCollection.find().limit(5).forEach(rawOrders::add);
        
        Map<String, Object> response = new HashMap<>();
        response.put("customers", rawCustomers);
        response.put("orders", rawOrders);
        response.put("message", "These are raw documents as stored in MongoDB. Encrypted fields appear as BinData.");
        
        log.info("Returning {} raw customers and {} raw orders", rawCustomers.size(), rawOrders.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cross-tenant-attempt")
    public ResponseEntity<Map<String, Object>> attemptCrossTenantDecryption(
            @RequestBody Map<String, String> request) {
        
        String attackerTenant = request.get("attackerTenant");
        String victimTenant = request.get("victimTenant");
        
        log.info("Simulating cross-tenant attack: {} trying to read {}'s data", attackerTenant, victimTenant);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            MongoDatabase plainDatabase = plainMongoClient.getDatabase(databaseName);
            MongoCollection<Document> plainCollection = plainDatabase.getCollection("customers");
            
            Document victimDoc = plainCollection.find(new Document("tenantId", victimTenant)).first();
            
            if (victimDoc == null) {
                response.put("success", false);
                response.put("error", "No documents found for victim tenant");
                return ResponseEntity.ok(response);
            }
            
            log.info("Retrieved victim document (encrypted): {}", victimDoc.toJson());
            
            MongoClient attackerClient = clientFactory.getClientForTenant(attackerTenant);
            MongoDatabase attackerDatabase = attackerClient.getDatabase(databaseName);
            MongoCollection<Document> attackerCollection = attackerDatabase.getCollection("customers");

            // Attempt 1: Try to decrypt by querying with victim's _id directly (bypassing tenantId filter)
            // This simulates an attacker trying to access data by knowing the document ID
            Document attemptedDecryption = attackerCollection.find(
                    new Document("_id", victimDoc.get("_id"))
            ).first();
            
            response.put("success", false);
            response.put("attackerTenant", attackerTenant);
            response.put("victimTenant", victimTenant);
            response.put("victimDocumentId", victimDoc.get("_id").toString());
            response.put("rawDocument", victimDoc);
            response.put("attemptedDecryption", attemptedDecryption);

            if (attemptedDecryption != null) {
                // Check if the encrypted fields are still encrypted (Binary data) or decrypted (String)
                Object nameField = attemptedDecryption.get("name");
                Object emailField = attemptedDecryption.get("email");

                boolean isNameEncrypted = nameField instanceof org.bson.types.Binary;
                boolean isEmailEncrypted = emailField instanceof org.bson.types.Binary;

                if (isNameEncrypted || isEmailEncrypted) {
                    // Fields are still encrypted - decryption failed (expected behavior)
                    response.put("decryptionResult", "✅ SUCCESS: Cryptographic Isolation Enforced!");
                    response.put("explanation", "Cross-tenant decryption failed because:\n\n" +
                            "1. Each tenant has their own master key\n" +
                            "2. Each tenant's DEK is encrypted with their own master key\n" +
                            "3. The attacker's client only has access to their own master key\n" +
                            "4. Without the victim's master key, the attacker cannot decrypt the victim's DEK\n" +
                            "5. Without the victim's DEK, the attacker cannot decrypt the victim's data\n\n" +
                            "Encrypted fields remain as Binary data, proving true cryptographic isolation!");
                    response.put("fieldsStillEncrypted", true);
                } else if (nameField != null && nameField instanceof String) {
                    // Fields were decrypted - this should only happen if same tenant
                    if (attackerTenant.equals(victimTenant)) {
                        response.put("decryptionResult", "ℹ️ EXPECTED: Same tenant can decrypt their own data");
                        response.put("explanation", "Attacker and victim are the same tenant, so decryption succeeds.");
                    } else {
                        response.put("decryptionResult", "⚠️ UNEXPECTED: Cross-tenant decryption succeeded");
                        response.put("explanation", "This should NOT happen with separate master keys per tenant!\n\n" +
                                "If you're seeing this, it means the master keys were not properly isolated.\n" +
                                "Please check the logs to ensure each tenant has a separate master key file.");
                        response.put("securityNote", "With separate master keys, cross-tenant decryption should be " +
                                "cryptographically impossible.");
                    }
                    response.put("fieldsStillEncrypted", false);
                } else {
                    response.put("decryptionResult", "❓ UNKNOWN: Fields are null or in unexpected format");
                    response.put("fieldsStillEncrypted", null);
                }
            } else {
                response.put("decryptionResult", "Document not found or query failed");
            }
            
        } catch (Exception e) {
            log.error("Cross-tenant decryption attempt failed with exception", e);
            response.put("success", false);
            response.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            response.put("message", "✅ Cryptographic isolation enforced! The attacker cannot decrypt the victim's data " +
                    "because they don't have access to the victim's master key.");
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/application-level-access-control")
    public ResponseEntity<Map<String, Object>> demonstrateApplicationLevelAccessControl(
            @RequestBody Map<String, String> request) {

        String requestingTenant = request.get("requestingTenant");

        log.info("Demonstrating application-level access control for tenant: {}", requestingTenant);

        Map<String, Object> response = new HashMap<>();

        try {
            // Get the tenant's encrypted client to decrypt their own data
            MongoClient tenantClient = clientFactory.getClientForTenant(requestingTenant);
            MongoDatabase encryptedDatabase = tenantClient.getDatabase(databaseName);
            MongoCollection<Document> encryptedCollection = encryptedDatabase.getCollection("customers");

            // Also get plain client to get ALL documents (including encrypted ones)
            MongoDatabase plainDatabase = plainMongoClient.getDatabase(databaseName);
            MongoCollection<Document> plainCollection = plainDatabase.getCollection("customers");

            // Scenario A: INCORRECT - Query without tenantId filter (security violation)
            // Get ALL documents from plain client first
            List<Document> allDocsPlain = new ArrayList<>();
            plainCollection.find().forEach(allDocsPlain::add);

            // Try to decrypt each document using the tenant's encrypted client
            List<Document> allCustomersNoFilter = new ArrayList<>();
            for (Document plainDoc : allDocsPlain) {
                String docTenantId = plainDoc.getString("tenantId");

                // Try to get the decrypted version for this tenant's documents
                if (docTenantId.equals(requestingTenant)) {
                    // This is the requesting tenant's document - get decrypted version
                    try {
                        Document decryptedDoc = encryptedCollection.find(
                            new Document("_id", plainDoc.get("_id"))
                        ).first();
                        if (decryptedDoc != null) {
                            allCustomersNoFilter.add(decryptedDoc);
                        } else {
                            allCustomersNoFilter.add(plainDoc);
                        }
                    } catch (Exception e) {
                        log.warn("Could not decrypt document: {}", e.getMessage());
                        allCustomersNoFilter.add(plainDoc);
                    }
                } else {
                    // This is another tenant's document - keep encrypted version
                    allCustomersNoFilter.add(plainDoc);
                }
            }

            long totalDocumentsInDb = allDocsPlain.size();

            // Scenario B: CORRECT - Query with tenantId filter (proper access control)
            // With filtering, only the requesting tenant's documents are returned (and decrypted)
            List<Document> tenantCustomersFiltered = new ArrayList<>();
            encryptedCollection.find(new Document("tenantId", requestingTenant))
                .forEach(tenantCustomersFiltered::add);

            response.put("success", true);
            response.put("requestingTenant", requestingTenant);

            // Scenario A results
            Map<String, Object> scenarioA = new HashMap<>();
            scenarioA.put("description", "❌ INCORRECT: Query without tenantId filter");
            scenarioA.put("query", "collection.find()");
            scenarioA.put("totalDocuments", allCustomersNoFilter.size());
            scenarioA.put("totalDocumentsInDatabase", totalDocumentsInDb);
            scenarioA.put("issue", String.format(
                "SECURITY VIOLATION: Returns ALL %d documents from ALL tenants! " +
                "Even though encryption prevents reading other tenants' sensitive fields, " +
                "the query still exposes that these documents exist. Application-level filtering is ESSENTIAL!",
                allCustomersNoFilter.size()
            ));
            scenarioA.put("customers", allCustomersNoFilter.stream()
                    .map(doc -> {
                        Map<String, Object> customer = new HashMap<>();
                        customer.put("tenantId", doc.getString("tenantId"));

                        // Try to get decrypted values, fall back to showing encrypted indicator
                        Object nameObj = doc.get("name");
                        Object emailObj = doc.get("email");

                        if (nameObj instanceof String) {
                            customer.put("name", nameObj);
                        } else {
                            customer.put("name", "[ENCRYPTED - Binary Data]");
                            customer.put("nameEncrypted", true);
                        }

                        if (emailObj instanceof String) {
                            customer.put("email", emailObj);
                        } else {
                            customer.put("email", "[ENCRYPTED - Binary Data]");
                            customer.put("emailEncrypted", true);
                        }

                        return customer;
                    })
                    .toList());
            response.put("scenarioA_NoFilter", scenarioA);

            // Scenario B results
            Map<String, Object> scenarioB = new HashMap<>();
            scenarioB.put("description", "✅ CORRECT: Query with tenantId filter");
            scenarioB.put("query", "collection.find({ tenantId: '" + requestingTenant + "' })");
            scenarioB.put("totalDocuments", tenantCustomersFiltered.size());
            scenarioB.put("success", "Returns ONLY " + tenantCustomersFiltered.size() + " customers for " + requestingTenant);
            scenarioB.put("customers", tenantCustomersFiltered.stream()
                    .map(doc -> {
                        Map<String, Object> customer = new HashMap<>();
                        customer.put("tenantId", doc.getString("tenantId"));
                        customer.put("name", doc.getString("name"));
                        customer.put("email", doc.getString("email"));
                        return customer;
                    })
                    .toList());
            response.put("scenarioB_WithFilter", scenarioB);

            // Defense in depth explanation
            Map<String, String> defenseInDepth = new HashMap<>();
            defenseInDepth.put("layer1",
                "🔐 Cryptographic Isolation (Separate Master Keys): " +
                "Each tenant has their own master key. Even if an attacker accesses the database directly, " +
                "they cannot decrypt other tenants' sensitive fields (name, email, phone, address). " +
                "Encrypted fields remain as binary data without the correct master key."
            );
            defenseInDepth.put("layer2", String.format(
                "🛡️ Application-Level Filtering (Always Filter by tenantId): " +
                "Your application code MUST always filter queries by tenantId. " +
                "This demo shows %d total documents exist, but only %d belong to %s. " +
                "Without filtering, the application would expose all %d documents!",
                totalDocumentsInDb, tenantCustomersFiltered.size(), requestingTenant, totalDocumentsInDb
            ));
            defenseInDepth.put("combined",
                "✅ Defense in Depth - Both Layers Are Essential: " +
                "Layer 1 (Encryption) protects sensitive field values. " +
                "Layer 2 (Filtering) prevents accessing other tenants' documents. " +
                "NEVER rely on encryption alone - always implement both layers!"
            );
            response.put("defenseInDepth", defenseInDepth);

            log.info("Application-level access control demo completed for tenant: {}", requestingTenant);

        } catch (Exception e) {
            log.error("Application-level access control demo failed", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}


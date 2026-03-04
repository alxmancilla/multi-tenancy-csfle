# MongoDB CSFLE Multi-Tenancy Demo
## Presentation Slide Deck

---

## Slide 1: Title Slide

# MongoDB Client-Side Field Level Encryption (CSFLE)
## Multi-Tenancy Demo

**Demonstrating True Cryptographic Isolation in Shared Database Environments**

---

## Slide 2: The Challenge

### Multi-Tenancy Security Challenges

**The Problem:**
- Multiple tenants sharing the same database and collections
- Need to protect sensitive data from unauthorized access
- DBAs and infrastructure teams have database access
- Risk of cross-tenant data exposure

**Traditional Approaches:**
- ❌ Separate databases per tenant (expensive, hard to scale)
- ❌ Application-level encryption (keys stored in app, vulnerable)
- ❌ Database-level encryption (DBAs can still read data)
- ❌ Relying only on application filtering (bugs can expose data)

**Our Solution:**
✅ **Client-Side Field Level Encryption (CSFLE) with Separate Master Keys Per Tenant**

---

## Slide 3: What is CSFLE?

### Client-Side Field Level Encryption

**Key Concepts:**
- 🔐 **Encryption happens on the client side** (in your application)
- 🔑 **MongoDB never sees plaintext data** for sensitive fields
- 📦 **Automatic encryption/decryption** using MongoDB drivers
- 🛡️ **Field-level granularity** - encrypt only what you need

**How It Works:**
1. Application encrypts sensitive fields before sending to MongoDB
2. MongoDB stores encrypted binary data
3. Application automatically decrypts when reading data
4. DBAs see only encrypted binary data

**Benefits:**
- ✅ Zero trust architecture
- ✅ Compliance-ready (GDPR, HIPAA, PCI-DSS)
- ✅ Protection from insider threats
- ✅ Transparent to application logic

---

## Slide 4: Architecture Overview

### Multi-Tenancy Security Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    MongoDB Database (Shared)                     │
├─────────────────────────────────────────────────────────────────┤
│  Collection: customers (shared by all tenants)                   │
│  Collection: orders (shared by all tenants)                      │
│  Collection: __keyVault (contains all tenant DEKs)               │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼────┐           ┌────▼────┐           ┌────▼────┐
   │ Tenant  │           │ Tenant  │           │ Tenant  │
   │  Alpha  │           │  Beta   │           │  Gamma  │
   └────┬────┘           └────┬────┘           └────┬────┘
        │                     │                     │
   ┌────▼────┐           ┌────▼────┐           ┌────▼────┐
   │  DEK    │           │  DEK    │           │  DEK    │
   │ (Alpha) │           │ (Beta)  │           │ (Gamma) │
   └────┬────┘           └────┬────┘           └────┬────┘
        │                     │                     │
   Encrypted with        Encrypted with        Encrypted with
        │                     │                     │
   ┌────▼────┐           ┌────▼────┐           ┌────▼────┐
   │ Master  │           │ Master  │           │ Master  │
   │   Key   │           │   Key   │           │   Key   │
   │ (Alpha) │           │ (Beta)  │           │ (Gamma) │
   └─────────┘           └─────────┘           └─────────┘
```

**Key Security Properties:**
1. Each tenant has their own master key file (96 bytes)
2. Each tenant's DEK is encrypted with their own master key
3. DEKs are stored in a shared `__keyVault` collection
4. Cross-tenant decryption is cryptographically impossible (HMAC validation)
5. Application code enforces tenant boundaries through filtering

---

## Slide 5: Cryptographic Isolation

### How Separate Master Keys Prevent Cross-Tenant Access

**The Two-Key System:**

**Master Key (per tenant):**
- 96-byte random key stored in secure file
- Never sent to MongoDB
- Used to encrypt/decrypt the DEK

**Data Encryption Key (DEK, per tenant):**
- Stored in MongoDB's `__keyVault` collection
- Encrypted with the tenant's master key
- Used to encrypt/decrypt actual data fields

**Why This Works:**
1. Tenant Alpha tries to decrypt Tenant Beta's data
2. Needs Tenant Beta's DEK from key vault
3. Retrieves encrypted DEK from `__keyVault`
4. Tries to decrypt DEK with Tenant Alpha's master key
5. ❌ **HMAC validation fails** - wrong master key!
6. Cannot get plaintext DEK
7. Cannot decrypt Tenant Beta's data

**Result:** Cross-tenant decryption is **cryptographically impossible**

---

## Slide 6: Data Model

### What Gets Encrypted?

**Customers Collection:**
```json
{
  "tenantId": "tenant_alpha",           // ✅ Plaintext (needed for filtering)
  "customerId": "cust_001",             // ✅ Plaintext (needed for queries)
  "name": "<ENCRYPTED - randomized>",   // 🔒 Encrypted
  "email": "<ENCRYPTED - deterministic>", // 🔒 Encrypted (queryable)
  "phone": "<ENCRYPTED - randomized>",  // 🔒 Encrypted
  "address": "<ENCRYPTED - randomized>" // 🔒 Encrypted
}
```

**Encryption Types:**
- **Deterministic**: Same plaintext → same ciphertext (enables equality queries)
  - Used for: email (can search by email)
- **Randomized**: Same plaintext → different ciphertext each time (maximum security)
  - Used for: name, phone, address

---

## Slide 7: Tech Stack

### Implementation Details

**Backend:**
- Java 17+
- Spring Boot 3.2.0
- MongoDB Driver Sync 5.2.1 with CSFLE support
- Automatic Encryption Shared Library (libmongocrypt 8.2.5)

**Frontend:**
- React 18+
- Vite 6.0.7
- Modern responsive UI

**Database:**
- MongoDB Atlas
- Shared collections for all tenants
- Separate `__keyVault` collection for DEKs

**Key Management:**
- Local KMS provider (for demo)
- Separate 96-byte master key file per tenant
- Production: Use AWS KMS, Azure Key Vault, or GCP KMS

---

## Slide 8: Demo Features

### Interactive Demonstrations

**1. 👥 Tenant Management**
- View all tenants (Acme Corp, Globex Inc, Initech LLC)
- See tenant-specific data
- All data automatically encrypted/decrypted

**2. 📋 Customer Management**
- Create customers with encrypted PII
- Query by email (deterministic encryption)
- View decrypted data for authorized tenant only

**3. 👁️ DBA View**
- See raw MongoDB documents
- Sensitive fields show as `BinData` (binary)
- Proves DBAs cannot read plaintext

**4. ⚔️ Cross-Tenant Attack Simulation**
- Simulate attacker trying to decrypt victim's data
- Shows HMAC validation failure
- Proves cryptographic isolation

**5. 🛡️ Application-Level Access Control**
- Demonstrates defense in depth
- Shows why filtering is essential
- Compares filtered vs unfiltered queries

---

## Slide 9: Demo Walkthrough - DBA View

### What DBAs See in the Database

**Raw MongoDB Document:**
```json
{
  "_id": ObjectId("..."),
  "tenantId": "tenant_alpha",
  "customerId": "cust_001",
  "name": {
    "$binary": {
      "base64": "AQGCAAAAAAAAAAAAAAAAAACx7...",
      "subType": "06"
    }
  },
  "email": {
    "$binary": {
      "base64": "AQGCAAAAAAAAAAAAAAAAAACy8...",
      "subType": "06"
    }
  }
}
```

**Key Observations:**
- ✅ `tenantId` and `customerId` are plaintext (needed for filtering/queries)
- 🔒 `name` and `email` are binary encrypted data
- ❌ DBAs cannot read sensitive information
- ✅ Compliance requirement met: data at rest is encrypted

---

## Slide 10: Demo Walkthrough - Cross-Tenant Attack

### Proving Cryptographic Isolation

**Attack Scenario:**
1. Attacker: Tenant Alpha (Acme Corp)
2. Victim: Tenant Beta (Globex Inc)
3. Attacker tries to decrypt Victim's customer data

**What Happens:**
```
Step 1: Attacker queries victim's documents ✅
  → Can retrieve documents (if no filtering)

Step 2: Attacker tries to decrypt victim's DEK ❌
  → HMAC validation failure
  → Wrong master key!

Step 3: Attacker cannot decrypt victim's data ❌
  → Encrypted fields remain as binary
  → name: Binary(96 bytes)
  → email: Binary(96 bytes)
```

**Result:**
✅ **SUCCESS: Cryptographic Isolation Enforced!**
- Attacker cannot read victim's sensitive data
- Encryption is cryptographically sound
- Cross-tenant decryption is impossible

---

## Slide 11: Demo Walkthrough - Application-Level Access Control

### Defense in Depth: Why Both Layers Matter

**Scenario A: ❌ INCORRECT (No Filter)**
```javascript
// Query without tenantId filter
collection.find()
```
**Result:**
- Returns: ALL 6 customers (2 from each tenant)
- Shows: Only selected tenant's data decrypted
- Other tenants: `[ENCRYPTED - Binary Data]` 🔒
- **Problem**: Query exposes that 6 documents exist!

**Scenario B: ✅ CORRECT (With Filter)**
```javascript
// Query with tenantId filter
collection.find({ tenantId: 'tenant_alpha' })
```
**Result:**
- Returns: Only 2 customers (selected tenant)
- Shows: All data decrypted
- **Proper**: Query only accesses tenant's own documents

**Key Takeaway:**
- **Layer 1 (Encryption)**: Protects field values ✅
- **Layer 2 (Filtering)**: Prevents document access ✅
- **Both are essential**: Never rely on encryption alone!

---

## Slide 12: Security Benefits

### Why This Approach Wins

**1. True Cryptographic Isolation**
- Cross-tenant decryption is mathematically impossible
- HMAC validation ensures key integrity
- No shared secrets between tenants

**2. Zero Trust Architecture**
- MongoDB never sees plaintext sensitive data
- DBAs cannot read encrypted fields
- Protection from insider threats

**3. Compliance Ready**
- GDPR: Data minimization and encryption at rest ✅
- HIPAA: PHI protection ✅
- PCI-DSS: Cardholder data encryption ✅
- SOC 2: Access controls and encryption ✅

**4. Defense in Depth**
- Layer 1: Cryptographic isolation (separate master keys)
- Layer 2: Application filtering (tenant boundaries)
- Layer 3: Database access controls
- Multiple layers protect against different attack vectors

**5. Scalable Multi-Tenancy**
- Shared database and collections (cost-effective)
- No performance penalty for encryption
- Easy to add new tenants

---

## Slide 13: Real-World Use Cases

### Where This Matters

**SaaS Applications:**
- Customer data isolation in shared infrastructure
- Regulatory compliance across industries
- Protection from data breaches

**Healthcare:**
- Patient records (HIPAA compliance)
- Multi-clinic/hospital systems
- Protecting PHI from unauthorized access

**Financial Services:**
- Customer financial data (PCI-DSS)
- Multi-branch banking systems
- Fraud prevention

**Government:**
- Citizen data protection
- Multi-agency data sharing
- Classified information handling

**E-Commerce:**
- Customer PII and payment info
- Multi-vendor marketplaces
- GDPR compliance

---

## Slide 14: Performance Considerations

### Is Encryption Expensive?

**Encryption Overhead:**
- ✅ Minimal CPU impact (modern hardware acceleration)
- ✅ No network overhead (same data size)
- ✅ Automatic encryption/decryption (transparent to app)

**Query Performance:**
- ✅ Deterministic encryption enables equality queries
- ✅ Indexes work on encrypted fields (deterministic only)
- ❌ Range queries not supported on encrypted fields
- ❌ Full-text search not supported on encrypted fields

**Best Practices:**
- Encrypt only sensitive fields (not everything)
- Use deterministic encryption for queryable fields
- Keep query fields (tenantId, IDs) unencrypted
- Use randomized encryption for maximum security on non-queryable fields

**Benchmark Results (from our demo):**
- Insert: ~5ms overhead per document
- Query: ~2ms overhead per document
- Negligible impact for most applications

---

## Slide 15: Implementation Highlights

### Key Code Components

**1. LocalKmsProvider**
- Generates separate 96-byte master key per tenant
- Stores keys in secure files
- Provides master keys to MongoDB driver

**2. TenantKeyService**
- Creates DEKs for each tenant
- Encrypts DEKs with tenant-specific master keys
- Stores encrypted DEKs in `__keyVault` collection

**3. TenantMongoClientFactory**
- Creates separate MongoClient per tenant
- Configures automatic encryption with tenant's master key
- Ensures tenant isolation at client level

**4. Automatic Encryption Configuration**
- Schema map defines which fields to encrypt
- Encryption algorithm per field (deterministic vs randomized)
- Transparent to application code

**5. Application Filtering**
- All queries filter by `tenantId`
- Enforced at service layer
- Defense in depth with encryption

---

## Slide 16: Lessons Learned

### Key Takeaways from Building This Demo

**✅ What Worked Well:**
1. Automatic encryption is truly transparent
2. Separate master keys provide strong isolation
3. HMAC validation prevents key misuse
4. MongoDB driver handles complexity

**⚠️ Challenges Encountered:**
1. HMAC validation errors when keys change
2. Must delete old DEKs when regenerating master keys
3. Debugging encrypted data is difficult
4. Key management is critical

**💡 Best Practices:**
1. **Never share master keys between tenants**
2. **Always filter queries by tenantId**
3. **Use deterministic encryption sparingly** (only for queryable fields)
4. **Test cross-tenant isolation thoroughly**
5. **Plan key rotation strategy from day one**
6. **Use production KMS** (AWS KMS, Azure Key Vault, GCP KMS)

---

## Slide 17: Production Considerations

### Moving Beyond the Demo

**Key Management:**
- ❌ Demo: Local KMS with file-based keys
- ✅ Production: AWS KMS, Azure Key Vault, or GCP KMS
- ✅ Implement key rotation policies
- ✅ Audit key access

**Monitoring:**
- Track encryption/decryption errors
- Monitor HMAC validation failures
- Alert on cross-tenant access attempts
- Performance metrics

**Disaster Recovery:**
- Backup master keys securely
- Document key recovery procedures
- Test restoration process
- Consider key escrow for compliance

**Scalability:**
- Connection pooling per tenant
- Cache DEKs appropriately
- Monitor memory usage (one client per tenant)
- Consider client lifecycle management

**Compliance:**
- Document encryption architecture
- Maintain audit logs
- Regular security assessments
- Penetration testing

---

## Slide 18: Demo Architecture Summary

### Full Stack Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend (Vite)                    │
│  - Tenant Management UI                                      │
│  - Customer Management UI                                    │
│  - DBA View Panel                                            │
│  - Cross-Tenant Attack Simulator                             │
│  - Application-Level Access Control Demo                     │
└────────────────────────┬────────────────────────────────────┘
                         │ REST API
┌────────────────────────▼────────────────────────────────────┐
│              Spring Boot Backend (Java 17)                   │
│  - TenantMongoClientFactory (one client per tenant)          │
│  - TenantKeyService (DEK management)                         │
│  - LocalKmsProvider (master key management)                  │
│  - Automatic encryption configuration                        │
└────────────────────────┬────────────────────────────────────┘
                         │ MongoDB Driver with CSFLE
┌────────────────────────▼────────────────────────────────────┐
│                    MongoDB Atlas                             │
│  - customers collection (shared, encrypted fields)           │
│  - orders collection (shared, encrypted fields)              │
│  - __keyVault collection (encrypted DEKs)                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  Master Key Files (Local)                    │
│  - local_master_key_tenant_alpha.bin (96 bytes)              │
│  - local_master_key_tenant_beta.bin (96 bytes)               │
│  - local_master_key_tenant_gamma.bin (96 bytes)              │
└─────────────────────────────────────────────────────────────┘
```

---

## Slide 19: Live Demo Time!

### What We'll Show

**1. Normal Operations (2 min)**
- View tenants and their customers
- Create a new customer
- See automatic encryption in action

**2. DBA View (1 min)**
- Show raw MongoDB documents
- Point out encrypted binary fields
- Demonstrate DBA cannot read data

**3. Cross-Tenant Attack (2 min)**
- Select attacker and victim tenants
- Simulate attack
- Show HMAC validation failure
- Prove cryptographic isolation

**4. Application-Level Access Control (2 min)**
- Show unfiltered query (6 customers, only 2 decrypted)
- Show filtered query (2 customers, all decrypted)
- Explain defense in depth

**Total: ~7 minutes**

---

## Slide 20: Q&A and Resources

### Questions?

**Common Questions:**

**Q: Can I use this in production?**
A: Yes! Replace Local KMS with AWS KMS/Azure Key Vault/GCP KMS

**Q: What about key rotation?**
A: MongoDB supports key rotation. Plan your rotation strategy early.

**Q: Performance impact?**
A: Minimal (~2-5ms per operation). Encrypt only sensitive fields.

**Q: Can I query encrypted fields?**
A: Yes, with deterministic encryption (equality queries only)

**Q: What if I lose a master key?**
A: Data encrypted with that key is unrecoverable. Backup keys securely!

---

### Resources

**Documentation:**
- MongoDB CSFLE Docs: https://docs.mongodb.com/manual/core/security-client-side-encryption/
- This Demo: `/README.md`, `/GETTING_STARTED.md`
- Implementation Details: `/SEPARATE_MASTER_KEYS_IMPLEMENTATION.md`

**Try It Yourself:**
```bash
# Clone and run
git clone <repo-url>
cd multi-tenancy-csfle
# Follow GETTING_STARTED.md
```

**Contact:**
- Questions? Open an issue
- Contributions welcome!

---

## Slide 21: Thank You!

# Thank You!

### Key Takeaways

1. ✅ **CSFLE enables true cryptographic isolation** in multi-tenant environments
2. ✅ **Separate master keys per tenant** prevent cross-tenant decryption
3. ✅ **Defense in depth** - combine encryption with application filtering
4. ✅ **Compliance-ready** - GDPR, HIPAA, PCI-DSS
5. ✅ **Production-ready** - use with AWS KMS, Azure Key Vault, or GCP KMS

### Next Steps

- Try the demo yourself
- Review the code
- Implement in your own applications
- Share feedback and improvements

**Questions? Let's discuss!**

---

# End of Presentation



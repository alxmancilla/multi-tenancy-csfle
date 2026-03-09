# MongoDB Client-Side Field Level Encryption (CSFLE) Multi-Tenancy Demo

A complete, runnable demonstration of MongoDB Client-Side Field Level Encryption (CSFLE) in a multi-tenancy scenario. This application shows how multiple tenants can share the same MongoDB database and collections while maintaining **true cryptographic isolation** through per-tenant Master Keys and Data Encryption Keys (DEKs).

> **🚀 New here?** Start with **[GETTING_STARTED.md](GETTING_STARTED.md)** for a quick 5-minute setup guide!
>
> **🐳 Want to run with Docker?** See **[DOCKER_SETUP.md](DOCKER_SETUP.md)** for one-command deployment!

## 🎯 Key Features

- **🔒 Separate Master Keys Per Tenant**: Each tenant has their own 96-byte master key file for true cryptographic isolation
- **🔑 Per-Tenant Data Encryption Keys (DEKs)**: Each tenant's DEK is encrypted with their own master key
- **📦 Shared Collections**: All tenants use the same `customers` and `orders` collections
- **🛡️ True Cryptographic Isolation**: Cross-tenant decryption is cryptographically impossible - HMAC validation ensures DEKs can only be decrypted with the correct master key
- **🔍 Deterministic & Randomized Encryption**: Email uses deterministic encryption (queryable), other fields use randomized encryption
- **👁️ DBA View Demo**: Shows what encrypted data looks like in the database
- **⚔️ Cross-Tenant Attack Demo**: Proves that one tenant cannot decrypt another tenant's data
- **🛡️ Application-Level Access Control Demo**: Shows how proper tenant filtering complements cryptographic isolation (defense in depth)

## 🏗️ Architecture

### Multi-Tenancy Security Model

This demo implements **Option 2: Separate Master Keys Per Tenant** for true cryptographic isolation:

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

**Key Security Properties**:
1. Each tenant has their own master key file (96 bytes)
2. Each tenant's DEK is encrypted with their own master key
3. DEKs are stored in a shared `__keyVault` collection
4. Cross-tenant decryption is impossible due to HMAC validation
5. Application code enforces tenant boundaries through filtering

### Tech Stack

- **Backend**: Java 17+, Spring Boot 3.x
- **Frontend**: React 18+ with Vite
- **Database**: MongoDB 8.0
- **MongoDB Driver**: mongodb-driver-sync 5.2.1 with CSFLE support
- **Build Tool**: Maven
- **Key Provider**: Local KMS (separate 96-byte master key per tenant for demo purposes)

### Tenants

Three pre-seeded tenants:
- `tenant_alpha` - Acme Corp
- `tenant_beta` - Globex Inc
- `tenant_gamma` - Initech LLC

### Data Model

**Customers Collection** (shared):
```json
{
  "tenantId": "<plaintext>",
  "customerId": "<plaintext>",
  "name": "<ENCRYPTED - randomized>",
  "email": "<ENCRYPTED - deterministic>",
  "phone": "<ENCRYPTED - randomized>",
  "address": "<ENCRYPTED - randomized>"
}
```

**Orders Collection** (shared):
```json
{
  "tenantId": "<plaintext>",
  "orderId": "<plaintext>",
  "customerId": "<plaintext>",
  "product": "<ENCRYPTED - randomized>",
  "amount": "<ENCRYPTED - randomized>",
  "status": "<ENCRYPTED - randomized>"
}
```

## 📋 Prerequisites

1. **Java 21 or higher**
   ```bash
   java -version
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **Node.js 18+ and npm**
   ```bash
   node -version
   npm -version
   ```

4. **MongoDB 8.0**
   ```bash
   mongod --version
   ```

5. **MongoDB Automatic Encryption Shared Library (Required)** ⚠️

   **This is critical!** The application will not work without this library.

   The MongoDB driver requires the Automatic Encryption Shared Library for CSFLE. This is the modern replacement for `mongocryptd`.

   **Download and Install:**

   Visit: https://www.mongodb.com/docs/manual/core/csfle/reference/shared-library/

   **For macOS (Intel):**
   ```bash
   # Download the library
   curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz

   # Extract
   tar -xvf mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz

   # Copy to a standard location
   sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
   ```

   **For macOS (Apple Silicon/ARM):**
   ```bash
   # Download the library
   curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz

   # Extract
   tar -xvf mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz

   # Copy to a standard location
   sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
   ```

   **For Linux:**
   ```bash
   # Download the library (Ubuntu/Debian example)
   curl -O https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-ubuntu2204-x86_64-enterprise-8.0.3.tgz

   # Extract
   tar -xvf mongo_crypt_shared_v1-ubuntu2204-x86_64-enterprise-8.0.3.tgz

   # Copy to a standard location
   sudo cp lib/mongo_crypt_v1.so /usr/local/lib/
   ```

   **For Windows:**
   ```powershell
   # Download from the MongoDB website
   # Extract mongo_crypt_v1.dll
   # Place in C:\Windows\System32 or add to PATH
   ```

   **Verify Installation:**
   ```bash
   # macOS/Linux
   ls -la /usr/local/lib/mongo_crypt_v1.*

   # The driver will automatically detect the library in standard locations
   ```

   **Alternative: Specify Custom Path**

   If you place the library in a custom location, you can specify it via environment variable:
   ```bash
   export MONGOCRYPT_SHARED_LIB_PATH=/path/to/mongo_crypt_v1.dylib
   ```

## 🚀 Setup Instructions

> **🐳 Prefer Docker?** Skip this section and see [DOCKER_SETUP.md](DOCKER_SETUP.md) for one-command deployment!

### Option 1: Docker Setup (Recommended for Quick Demo)

The easiest way to run the entire application:

```bash
# One command to start everything
docker-compose up --build
```

This will:
- Start MongoDB 8.0 in a container
- Build and start the backend (Java 17 + Spring Boot)
- Build and start the frontend (React + Nginx)
- Configure networking and health checks

**Access the application:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- MongoDB: localhost:27017

**See [DOCKER_SETUP.md](DOCKER_SETUP.md) for detailed Docker instructions.**

---

### Option 2: Manual Setup (For Development)

#### 1. Start MongoDB

Ensure MongoDB is running on `localhost:27017`:

```bash
mongod --dbpath /path/to/your/data/directory
```

Or if using MongoDB as a service:
```bash
# macOS
brew services start mongodb-community

# Linux
sudo systemctl start mongod
```

#### 2. Backend Setup

Navigate to the backend directory and build the project:

```bash
cd backend
mvn clean install
```

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

The backend will:
- Generate **separate master key files** for each tenant on first run:
  - `local_master_key_tenant_alpha.bin`
  - `local_master_key_tenant_beta.bin`
  - `local_master_key_tenant_gamma.bin`
- Create Data Encryption Keys for each tenant in the `__keyVault` collection
- Each DEK is encrypted with its tenant's specific master key
- Seed sample data (2 customers and 2 orders per tenant)
- Start the API server on `http://localhost:8080`

**Expected console output:**
```
Initializing per-tenant master keys...
Generating new master key for tenant 'tenant_alpha' and saving to: ./local_master_key_tenant_alpha.bin
Master key initialized for tenant: tenant_alpha
Generating new master key for tenant 'tenant_beta' and saving to: ./local_master_key_tenant_beta.bin
Master key initialized for tenant: tenant_beta
Generating new master key for tenant 'tenant_gamma' and saving to: ./local_master_key_tenant_gamma.bin
Master key initialized for tenant: tenant_gamma
All tenant master keys initialized successfully

Initializing Data Encryption Keys for all tenants with separate master keys...
Creating new DEK for tenant 'tenant_alpha': ...
Tenant 'tenant_alpha' DEK ID: ...
Creating new DEK for tenant 'tenant_beta': ...
Tenant 'tenant_beta' DEK ID: ...
Creating new DEK for tenant 'tenant_gamma': ...
Tenant 'tenant_gamma' DEK ID: ...
All tenant Data Encryption Keys initialized successfully with separate master keys

Seeding data for tenant: tenant_alpha
...
Started FledemoApplication in X.XXX seconds
```

#### 3. Frontend Setup

In a new terminal, navigate to the frontend directory:

```bash
cd frontend
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will be available at `http://localhost:3000`

## 🎮 Using the Application

### 1. Tenant Selector
- Switch between the three tenants (Alpha, Beta, Gamma)
- All data views are scoped to the selected tenant

### 2. Customers Panel
- View decrypted customers for the active tenant
- Add new customers (all sensitive fields are automatically encrypted)
- Search by email using deterministic encryption (enables exact-match queries)

### 3. Orders Panel
- View decrypted orders for the active tenant
- Create new orders linked to customers
- All sensitive fields (product, amount, status) are encrypted

### 4. DBA View Panel
- Click "View Raw MongoDB Documents"
- See how data appears in the database
- Notice that sensitive fields show as `BinData` (encrypted binary)
- Demonstrates that even DBAs cannot read the plaintext data

### 5. Cross-Tenant Attack Panel
- Select an attacker tenant and a victim tenant
- Click "Simulate Attack"
- **Expected Result**: "✅ SUCCESS: Cryptographic Isolation Enforced!"
- Encrypted fields remain as Binary data because:
  - Each tenant has their own master key
  - The attacker's client cannot decrypt the victim's DEK (HMAC validation failure)
  - Without the victim's DEK, the attacker cannot decrypt the victim's data
- Proves true cryptographic isolation between tenants

### 6. Application-Level Access Control Panel
- Select a tenant (e.g., Acme Corp / tenant_alpha)
- Click "Demonstrate Access Control"
- See two scenarios side-by-side:
  - **Scenario A (❌ INCORRECT - No Filter)**:
    - Query: `collection.find()` (no tenantId filter)
    - Returns: ALL 6 customers from ALL tenants
    - Shows: Only the selected tenant's data is decrypted (real names/emails)
    - Other tenants' data shows as `[ENCRYPTED - Binary Data]` with 🔒 badge
    - **Security Issue**: Even though encryption protects the data, the query exposes that these documents exist!
  - **Scenario B (✅ CORRECT - With Filter)**:
    - Query: `collection.find({ tenantId: 'tenant_alpha' })`
    - Returns: Only 2 customers from the selected tenant
    - Shows: Only the selected tenant's decrypted data
    - **Proper Security**: Query only accesses the tenant's own documents
- Demonstrates **defense in depth**:
  - **Layer 1 (Cryptographic Isolation)**: Separate master keys prevent decrypting other tenants' sensitive fields
  - **Layer 2 (Application Filtering)**: Always filter by `tenantId` to prevent accessing other tenants' documents
  - **Both layers are essential**: Encryption alone isn't enough - you must filter queries!

## 🔐 Security Highlights

### Cryptographic Isolation (Option 2: Separate Master Keys)

1. **Separate Master Keys Per Tenant**:
   - Each tenant has their own 96-byte master key file
   - Master keys are never shared between tenants
   - Files: `local_master_key_tenant_alpha.bin`, `local_master_key_tenant_beta.bin`, `local_master_key_tenant_gamma.bin`

2. **Per-Tenant Data Encryption Keys (DEKs)**:
   - Each tenant has a unique DEK stored in the `__keyVault` collection
   - Each DEK is encrypted with its tenant's specific master key
   - DEKs cannot be decrypted without the correct master key (HMAC validation)

3. **True Cryptographic Isolation**:
   - Cross-tenant decryption is **cryptographically impossible**
   - Even if an attacker retrieves another tenant's DEK from the key vault, they cannot decrypt it
   - HMAC validation ensures the DEK was encrypted with the correct master key

### Encryption Methods

4. **Client-Side Encryption**: All encryption/decryption happens in the application, not on the MongoDB server

5. **Deterministic Encryption**: Email field uses deterministic encryption to enable equality queries

6. **Randomized Encryption**: Other sensitive fields use randomized encryption for maximum security

### Defense in Depth

7. **Application-Level Access Control**:
   - All queries MUST filter by `tenantId`
   - Prevents accessing other tenants' documents
   - Even with encryption, unfiltered queries expose that documents exist
   - Complements cryptographic isolation for maximum security

8. **Multi-Layer Security (Defense in Depth)**:
   - **Layer 1 (Cryptographic Isolation)**: Separate master keys prevent decrypting other tenants' sensitive fields
     - Without the correct master key, encrypted fields remain as binary data
     - HMAC validation ensures DEKs can only be decrypted with the correct master key
   - **Layer 2 (Application Filtering)**: Always filter queries by `tenantId`
     - Prevents queries from accessing other tenants' documents
     - Essential even with encryption - never rely on encryption alone!
   - **Layer 3**: Database access controls (not shown in this demo)
   - **All layers work together**: Encryption protects field values, filtering prevents document access

## 📁 Project Structure

```
.
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/fledemo/
│       ├── FledemoApplication.java
│       ├── config/
│       │   ├── CorsConfig.java
│       │   ├── LocalKmsProvider.java
│       │   ├── MongoConfig.java
│       │   └── TenantMongoClientFactory.java
│       ├── keyvault/
│       │   └── TenantKeyService.java
│       ├── model/
│       │   ├── Customer.java
│       │   └── Order.java
│       ├── service/
│       │   ├── CustomerService.java
│       │   ├── OrderService.java
│       │   └── DataSeeder.java
│       ├── controller/
│       │   ├── TenantController.java
│       │   ├── CustomerController.java
│       │   ├── OrderController.java
│       │   └── DemoController.java
│       └── dto/
│           ├── CustomerRequest.java
│           ├── CustomerResponse.java
│           ├── OrderRequest.java
│           └── OrderResponse.java
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx
        ├── App.css
        ├── main.jsx
        ├── components/
        │   ├── TenantSelector.jsx
        │   ├── CustomersPanel.jsx
        │   ├── OrdersPanel.jsx
        │   ├── DbaViewPanel.jsx
        │   ├── CrossTenantPanel.jsx
        │   └── ApplicationLevelAccessPanel.jsx
        └── services/
            └── api.js
```

## 🔧 API Endpoints

### Tenants
- `GET /api/v1/tenants` - List all tenants

### Customers
- `POST /api/v1/tenants/{tenantId}/customers` - Create customer
- `GET /api/v1/tenants/{tenantId}/customers` - List customers
- `GET /api/v1/tenants/{tenantId}/customers/search?email=` - Search by email

### Orders
- `POST /api/v1/tenants/{tenantId}/orders` - Create order
- `GET /api/v1/tenants/{tenantId}/orders` - List orders
- `GET /api/v1/tenants/{tenantId}/orders/customer/{customerId}` - Orders by customer

### Demo Endpoints
- `GET /api/v1/demo/raw-documents` - View raw encrypted documents
- `POST /api/v1/demo/cross-tenant-attempt` - Simulate cross-tenant attack
- `POST /api/v1/demo/application-level-access-control` - Demonstrate application-level filtering

## 🧪 Testing the Demo

### 1. Verify Encryption
- Create a customer in tenant_alpha
- Go to "DBA View" and click "View Raw MongoDB Documents"
- Confirm that sensitive fields appear as `$binary` objects
- This proves data is encrypted at rest

### 2. Test Deterministic Encryption
- Note a customer's email in the Customers panel
- Use the search feature to find them by email
- This works because email uses deterministic encryption
- Demonstrates queryable encryption

### 3. Verify Cryptographic Isolation (Separate Master Keys)
- Go to "Cross-Tenant Attack" panel
- Select tenant_beta as attacker and tenant_alpha as victim
- Click "Simulate Attack"
- **Expected Result**: "✅ SUCCESS: Cryptographic Isolation Enforced!"
- Encrypted fields remain as Binary data
- This proves that:
  - Each tenant has their own master key
  - The attacker cannot decrypt the victim's DEK (HMAC validation fails)
  - Without the victim's DEK, the attacker cannot decrypt the victim's data
  - Cross-tenant decryption is cryptographically impossible

### 4. Verify Application-Level Access Control
- Go to "Application-Level Access Control" panel
- Select a tenant (e.g., Acme Corp / tenant_alpha)
- Click "Demonstrate Access Control"
- Observe two scenarios:
  - **Scenario A (❌ INCORRECT - No Filter)**:
    - Query without `tenantId` filter returns ALL 6 customers (2 from each tenant)
    - Only the selected tenant's customers show decrypted data (e.g., "Alice Anderson - alice@acmecorp.com")
    - Other tenants' customers show as `[ENCRYPTED - Binary Data]` with 🔒 badge
    - **Key Point**: Even though encryption protects the data, the query still exposes that 6 documents exist!
  - **Scenario B (✅ CORRECT - With Filter)**:
    - Query with `tenantId` filter returns only 2 customers (from selected tenant)
    - All data is decrypted and readable
    - **Key Point**: The query only accesses the tenant's own documents
- **Expected Results**:
  - Scenario A shows: 2 decrypted customers (tenant_alpha) + 4 encrypted customers (tenant_beta, tenant_gamma)
  - Scenario B shows: 2 decrypted customers (tenant_alpha only)
- This demonstrates:
  - **Cryptographic isolation works**: Other tenants' data cannot be decrypted (shows as binary)
  - **Application filtering is essential**: Without filtering, the query exposes all documents
  - **Defense in depth**: Both layers work together for maximum security

### 5. Verify Separate Master Key Files
After starting the backend, verify that three separate master key files were created:

```bash
cd backend
ls -la local_master_key*.bin

# Expected output:
# local_master_key_tenant_alpha.bin
# local_master_key_tenant_beta.bin
# local_master_key_tenant_gamma.bin

# Verify they are different files with different checksums:
shasum local_master_key*.bin

# Each file should have a unique checksum
```

## 🛠️ Troubleshooting

### Backend won't start
- Ensure MongoDB is running on localhost:27017
- Check that Java 17+ is installed
- **Verify the Automatic Encryption Shared Library is installed:**
  ```bash
  # macOS/Linux
  ls -la /usr/local/lib/mongo_crypt_v1.*

  # Should show the library file
  ```
- If library is in a custom location, set environment variable:
  ```bash
  export MONGOCRYPT_SHARED_LIB_PATH=/path/to/mongo_crypt_v1.dylib
  mvn spring-boot:run
  ```

### Frontend can't connect to backend
- Ensure backend is running on port 8080
- Check CORS configuration in `CorsConfig.java`
- Verify frontend is running on port 3000

### Encryption errors
- Ensure the master key files exist and are readable:
  - `local_master_key_tenant_alpha.bin`
  - `local_master_key_tenant_beta.bin`
  - `local_master_key_tenant_gamma.bin`
- Check that the `__keyVault` collection has DEKs for all tenants
- Verify MongoDB driver version supports CSFLE
- **Check for Automatic Encryption Shared Library errors in logs:**
  ```
  Look for: "mongocrypt_t" or "crypt_shared" in error messages
  Solution: Reinstall the Automatic Encryption Shared Library
  ```

### HMAC validation failure error
If you see `HMAC validation failure` error when starting the application:

**This is actually a GOOD sign!** It means the separate master keys are working correctly.

**Cause**: Old DEKs in the key vault were encrypted with a different master key (or old shared master key).

**Solution**: Clean up the database and restart:

1. **Delete old collections** (using MongoDB Compass, Atlas UI, or mongosh):
   ```bash
   # Using mongosh
   mongosh "mongodb://localhost:27017/fle_demo" --eval "
     db.__keyVault.drop();
     db.customers.drop();
     db.orders.drop();
     print('Collections dropped successfully');
   "
   ```

2. **Delete old master key files**:
   ```bash
   cd backend
   rm -f local_master_key*.bin
   ```

3. **Restart the application**:
   ```bash
   mvn spring-boot:run
   ```

The application will:
- Generate fresh master key files for each tenant
- Create new DEKs encrypted with the correct master keys
- Seed fresh data
- Start successfully

## 📚 Learn More

### Documentation in This Repository
- [`GETTING_STARTED.md`](GETTING_STARTED.md) - Quick 5-minute setup guide
- [`DOCKER_SETUP.md`](DOCKER_SETUP.md) - **Docker deployment guide (one-command setup)**
- [`SEPARATE_MASTER_KEYS_IMPLEMENTATION.md`](SEPARATE_MASTER_KEYS_IMPLEMENTATION.md) - Detailed guide on separate master keys implementation
- [`ARCHITECTURE.md`](ARCHITECTURE.md) - Detailed architecture documentation
- [`QUICKSTART.md`](QUICKSTART.md) - Quick start guide
- [`PROJECT_SUMMARY.md`](PROJECT_SUMMARY.md) - Complete project overview
- [`PRESENTATION.md`](PRESENTATION.md) - Full slide deck for team presentations
- [`EXECUTIVE_SUMMARY.md`](EXECUTIVE_SUMMARY.md) - One-page executive summary

### External Resources
- [MongoDB Client-Side Field Level Encryption](https://www.mongodb.com/docs/manual/core/csfle/)
- [MongoDB Automatic Encryption Shared Library](https://www.mongodb.com/docs/manual/core/csfle/reference/shared-library/)
- [MongoDB Queryable Encryption](https://www.mongodb.com/docs/manual/core/queryable-encryption/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)

## 📄 License

This is a demonstration project for educational purposes.


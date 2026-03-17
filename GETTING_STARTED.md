# Getting Started with MongoDB CSFLE Multi-Tenancy Demo

## 🎯 What This Demo Does

This application demonstrates **MongoDB Client-Side Field Level Encryption (CSFLE)** in a multi-tenant SaaS scenario where:

- ✅ Multiple tenants share the same database and collections
- ✅ Each tenant's data is encrypted with their own Data Encryption Key (DEK)
- ✅ Tenants cannot decrypt each other's data (cryptographic isolation)
- ✅ Encryption/decryption happens automatically on the client side
- ✅ MongoDB server never sees plaintext sensitive data

### 🚀 Production-Ready Features

This demo includes enterprise-grade improvements:
- **📊 Database Indexes**: Optimized multi-tenant queries
- **🔧 Externalized Schemas**: JSON-based encryption configuration
- **💾 LRU Client Caching**: Limits active clients to prevent resource exhaustion
- **🔌 Connection Pooling**: Configured per-tenant connection limits
- **🔄 Auto-Recovery**: Handles HMAC validation failures gracefully
- **⚡ Bypass Encryption**: Performance optimization for non-sensitive operations

See the **Production Improvements & Roadmap** section in [README.md](README.md) for details.

## 🚀 Quick Start (5 Minutes)

### Step 1: Install Prerequisites

```bash
# Check what you have
java -version    # Need 21+
mvn -version     # Need 3.6+
node --version   # Need 18+
mongod --version # Need 8.0
```

**Or use Docker** (recommended for easy setup):
```bash
# Copy environment file
cp .env.example .env

# Start everything with Docker
docker-compose up --build

# Access at http://localhost:3000
```

See [DOCKER_SETUP.md](DOCKER_SETUP.md) for full Docker instructions.

### Step 2: Install Automatic Encryption Shared Library ⚠️ CRITICAL

**macOS (Intel):**
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

**macOS (Apple Silicon):**
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

**Linux (Ubuntu):**
```bash
curl -O https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-ubuntu2204-x86_64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-ubuntu2204-x86_64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.so /usr/local/lib/
```

**Verify:**
```bash
ls -la /usr/local/lib/mongo_crypt_v1.*
# Should show the library file
```

### Step 3: Start MongoDB

```bash
# Terminal 1
mongod --dbpath /path/to/data
```

### Step 4: Start Backend

```bash
# Terminal 2
cd backend
mvn spring-boot:run

# Wait for:
# ✓ All tenant Data Encryption Keys initialized successfully
# ✓ Started FledemoApplication in X.XXX seconds
```

### Step 5: Start Frontend

```bash
# Terminal 3
cd frontend
npm install
npm run dev

# Open browser to: http://localhost:3000
```

## 🎮 Try It Out

### 1. Create a Customer
- Select **tenant_alpha**
- Go to **Customers** tab
- Fill in: Name, Email, Phone, Address
- Click **Create Customer**
- ✅ Customer appears in the table

### 2. Search by Email (Deterministic Encryption)
- Use the search form
- Enter an existing email
- ✅ Customer is found (proves deterministic encryption works!)

### 3. View Raw Encrypted Data
- Go to **DBA View (Raw Data)** tab
- Click **View Raw MongoDB Documents**
- ✅ See encrypted fields as `$binary` objects
- ✅ Sensitive data is NOT readable

### 4. Test Cross-Tenant Attack
- Go to **Cross-Tenant Attack** tab
- Select different attacker and victim tenants
- Click **Attempt Cross-Tenant Decryption**
- ✅ See "Cryptographic Isolation Enforced!" message
- ✅ Decryption fails (proves tenant isolation works!)

## 📚 Documentation

### Essential Reading
1. **[README.md](README.md)** - Complete documentation
2. **[QUICKSTART.md](QUICKSTART.md)** - Quick reference guide
3. **[SEPARATE_MASTER_KEYS_IMPLEMENTATION.md](SEPARATE_MASTER_KEYS_IMPLEMENTATION.md)** - Separate master keys implementation

### Deep Dive
4. **[ARCHITECTURE.md](ARCHITECTURE.md)** - How it works
5. **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - Complete project overview
6. **[PRESENTATION.md](PRESENTATION.md)** - Full slide deck for presentations
7. **[EXECUTIVE_SUMMARY.md](EXECUTIVE_SUMMARY.md)** - One-page executive summary

## 🔧 Troubleshooting

### Backend won't start

**Error: "Could not find crypt_shared library"**
```bash
# Install the Automatic Encryption Shared Library
# Download from MongoDB website and place in /usr/local/lib/
```

**Error: "Connection refused to localhost:27017"**
```bash
# Start MongoDB
mongod --dbpath /path/to/data
```

### Frontend can't connect

**Error: Network errors in browser console**
```bash
# Ensure backend is running on port 8080
curl http://localhost:8080/api/v1/tenants
```

### More Help
- See [README.md](README.md) troubleshooting section

## 🎓 What You'll Learn

### 1. Client-Side Field Level Encryption (CSFLE)
- How to encrypt data before it reaches MongoDB
- Deterministic vs randomized encryption
- When to use each encryption type

### 2. Multi-Tenancy with Cryptographic Isolation
- Per-tenant Data Encryption Keys (DEKs)
- Shared collections with isolated data
- Why database-level isolation isn't enough

### 3. Key Management
- Master key hierarchy
- Data Encryption Key (DEK) management
- Key vault collection structure

### 4. Modern MongoDB Features
- Automatic Encryption Shared Library
- AutoEncryptionSettings configuration
- MongoDB Java Driver 5.x CSFLE support

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────┐
│   React Frontend (Port 3000)        │
└─────────────────┬───────────────────┘
                  │ HTTP/REST
┌─────────────────▼───────────────────┐
│   Spring Boot Backend (Port 8080)   │
│   ┌─────────────────────────────┐   │
│   │ MongoDB Java Driver 5.2.0   │   │
│   │  ┌───────────────────────┐  │   │
│   │  │ libmongocrypt         │  │   │ ← Automatic Encryption
│   │  │ (Shared Library)      │  │   │   Shared Library
│   │  └───────────────────────┘  │   │
│   └─────────────────────────────┘   │
└─────────────────┬───────────────────┘
                  │ Encrypted Data
┌─────────────────▼───────────────────┐
│   MongoDB 8.0                       │
│   ├─ customers (encrypted)          │
│   ├─ orders (encrypted)             │
│   └─ __keyVault (DEKs)              │
└─────────────────────────────────────┘
```

## 🎯 Key Features

### ✅ Automatic Encryption
- Fields are encrypted/decrypted automatically
- No manual encryption code needed
- Transparent to application logic

### ✅ Queryable Encryption
- Email field uses deterministic encryption
- Enables exact-match queries
- Search by email works seamlessly

### ✅ Tenant Isolation
- Each tenant has their own DEK
- Tenants cannot decrypt each other's data
- Cryptographic guarantee, not just access control

### ✅ Demo Features
- DBA view shows raw encrypted data
- Cross-tenant attack simulation
- Complete CRUD operations
- Real-time tenant switching

## 📊 Sample Data

The demo includes 3 pre-seeded tenants:

| Tenant ID | Company Name | Customers | Orders |
|-----------|--------------|-----------|--------|
| tenant_alpha | Acme Corp | 2 | 2 |
| tenant_beta | Globex Inc | 2 | 2 |
| tenant_gamma | Initech LLC | 2 | 2 |

## 🔐 Encryption Details

### Encrypted Fields

**Customers Collection:**
- `name` - Randomized encryption
- `email` - **Deterministic encryption** (queryable)
- `phone` - Randomized encryption
- `address` - Randomized encryption

**Orders Collection:**
- `product` - Randomized encryption
- `amount` - Randomized encryption
- `status` - Randomized encryption

### Plaintext Fields
- `tenantId` - Used for filtering
- `customerId` - Used for relationships
- `_id` - MongoDB document ID

## 🚀 Next Steps

1. ✅ Complete the Quick Start above
2. ✅ Try all demo features
3. ✅ View raw encrypted data in DBA view
4. ✅ Test cross-tenant attack simulation
5. ✅ Read [ARCHITECTURE.md](ARCHITECTURE.md) to understand how it works
6. ✅ Explore the code in `backend/src/` and `frontend/src/`

## 💡 Production Considerations

This is a **demo application**. For production use:

- ❌ Don't use local KMS (use AWS KMS, Azure Key Vault, or GCP Cloud KMS)
- ❌ Don't store master key in a file
- ✅ Use proper key rotation policies
- ✅ Implement audit logging
- ✅ Use MongoDB Atlas for managed encryption
- ✅ Follow MongoDB security best practices

## 📞 Need Help?

- 📖 Read [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture
- 📖 Read [SEPARATE_MASTER_KEYS_IMPLEMENTATION.md](SEPARATE_MASTER_KEYS_IMPLEMENTATION.md) for implementation details
- 📖 Read [PRESENTATION.md](PRESENTATION.md) for a full presentation slide deck
- 🌐 Visit [MongoDB CSFLE Documentation](https://www.mongodb.com/docs/manual/core/csfle/)

---

**Ready to start?** Follow the Quick Start above! 🚀


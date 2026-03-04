# Getting Started with MongoDB CSFLE Multi-Tenancy Demo

## 🎯 What This Demo Does

This application demonstrates **MongoDB Client-Side Field Level Encryption (CSFLE)** in a multi-tenant SaaS scenario where:

- ✅ Multiple tenants share the same database and collections
- ✅ Each tenant's data is encrypted with their own Data Encryption Key (DEK)
- ✅ Tenants cannot decrypt each other's data (cryptographic isolation)
- ✅ Encryption/decryption happens automatically on the client side
- ✅ MongoDB server never sees plaintext sensitive data

## 🚀 Quick Start (5 Minutes)

### Step 1: Install Prerequisites

```bash
# Check what you have
java -version    # Need 17+
mvn -version     # Need 3.6+
node -version    # Need 18+
mongod --version # Need 8.0
```

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

📖 **Detailed instructions:** See [`INSTALL_CRYPT_SHARED_LIBRARY.md`](INSTALL_CRYPT_SHARED_LIBRARY.md)

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
2. **[INSTALL_CRYPT_SHARED_LIBRARY.md](INSTALL_CRYPT_SHARED_LIBRARY.md)** - Library installation guide
3. **[SETUP_CHECKLIST.md](SETUP_CHECKLIST.md)** - Step-by-step verification

### Deep Dive
4. **[ARCHITECTURE.md](ARCHITECTURE.md)** - How it works
5. **[CRYPT_SHARED_VS_MONGOCRYPTD.md](CRYPT_SHARED_VS_MONGOCRYPTD.md)** - Modern vs deprecated approach
6. **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - Complete project overview
7. **[QUICKSTART.md](QUICKSTART.md)** - Quick reference guide

## 🔧 Troubleshooting

### Backend won't start

**Error: "Could not find crypt_shared library"**
```bash
# Install the Automatic Encryption Shared Library
# See INSTALL_CRYPT_SHARED_LIBRARY.md
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
- See [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) for detailed verification
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

- 📖 Read [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) for step-by-step verification
- 📖 Read [INSTALL_CRYPT_SHARED_LIBRARY.md](INSTALL_CRYPT_SHARED_LIBRARY.md) for library installation
- 📖 Read [CRYPT_SHARED_VS_MONGOCRYPTD.md](CRYPT_SHARED_VS_MONGOCRYPTD.md) to understand the encryption library
- 🌐 Visit [MongoDB CSFLE Documentation](https://www.mongodb.com/docs/manual/core/csfle/)

---

**Ready to start?** Follow the Quick Start above! 🚀


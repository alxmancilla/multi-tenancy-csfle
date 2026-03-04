# Quick Start Guide

## Prerequisites Check

```bash
# Check Java version (need 17+)
java -version

# Check Maven
mvn -version

# Check Node.js (need 18+)
node -version

# Check MongoDB (need 8.0)
mongod --version

# Check Automatic Encryption Shared Library
ls -la /usr/local/lib/mongo_crypt_v1.*
```

## Install Automatic Encryption Shared Library

**Required for CSFLE to work!**

### macOS (Intel):
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

### macOS (Apple Silicon):
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

### Linux (Ubuntu):
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

## Start in 3 Steps

### 1. Start MongoDB
```bash
mongod --dbpath /path/to/data
# OR if using brew on macOS:
brew services start mongodb-community
```

### 2. Start Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Wait for:
```
✓ Local master key generated
✓ Tenant DEKs initialized
✓ Sample data seeded
✓ Started FledemoApplication
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

Open browser to: **http://localhost:3000**

## What to Try

1. **View Encrypted Data**
   - Switch between tenants (Alpha, Beta, Gamma)
   - See decrypted customer and order data

2. **See Raw Database View**
   - Click "DBA View (Raw Data)" tab
   - Click "View Raw MongoDB Documents"
   - Notice sensitive fields are `BinData` (encrypted)

3. **Test Cross-Tenant Isolation**
   - Click "Cross-Tenant Attack" tab
   - Select different attacker/victim tenants
   - Click "Attempt Cross-Tenant Decryption"
   - See the decryption failure

4. **Test Deterministic Encryption**
   - Go to Customers panel
   - Search for a customer by email
   - This works because email uses deterministic encryption

## Verify It's Working

### Check MongoDB Collections
```bash
mongosh
use fle_demo

# Should see 3 DEKs (one per tenant)
db.__keyVault.countDocuments()

# Should see 6 customers (2 per tenant)
db.customers.countDocuments()

# Should see 6 orders (2 per tenant)
db.orders.countDocuments()

# View raw encrypted data
db.customers.findOne()
```

### Check API
```bash
# List tenants
curl http://localhost:8080/api/v1/tenants

# Get customers for tenant_alpha
curl http://localhost:8080/api/v1/tenants/tenant_alpha/customers
```

## Troubleshooting

**Backend fails to start:**
- Ensure MongoDB is running
- Check port 8080 is available
- **Verify Automatic Encryption Shared Library is installed:**
  ```bash
  ls -la /usr/local/lib/mongo_crypt_v1.*
  ```
- If missing, install it (see "Install Automatic Encryption Shared Library" section above)

**Frontend can't connect:**
- Ensure backend is running on port 8080
- Check port 3000 is available
- Clear browser cache

**Encryption errors:**
- Check backend logs for "mongocrypt" or "crypt_shared" errors
- Verify library is in the correct location
- Delete `local_master_key.bin` and restart backend
- Drop `fle_demo` database and restart backend
- Check MongoDB driver version in pom.xml

## Clean Reset

```bash
# Stop backend and frontend (Ctrl+C)

# Drop database
mongosh
use fle_demo
db.dropDatabase()
exit

# Delete master key
rm backend/local_master_key.bin

# Restart backend - it will regenerate everything
cd backend
mvn spring-boot:run
```


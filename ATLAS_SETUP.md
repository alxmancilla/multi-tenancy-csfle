# MongoDB Atlas Setup Guide for CSFLE Demo

This guide helps you configure the MongoDB CSFLE Multi-Tenancy Demo to work with MongoDB Atlas.

---

## 📋 Prerequisites

1. **MongoDB Atlas Account**: [Sign up here](https://www.mongodb.com/cloud/atlas/register)
2. **MongoDB 8.0+ Cluster**: CSFLE requires MongoDB 8.0 or higher
3. **Network Access**: Your IP must be whitelisted
4. **Database User**: With read/write permissions

---

## 🚀 Step-by-Step Setup

### Step 1: Create MongoDB Atlas Cluster

1. Log in to [MongoDB Atlas](https://cloud.mongodb.com)
2. Click **"Build a Database"**
3. Choose a tier:
   - **M0 (Free)**: Good for testing (limited to 512MB)
   - **M10+**: Recommended for development (better performance)
4. Select **MongoDB 8.0** or higher
5. Choose your cloud provider and region
6. Click **"Create Cluster"**

---

### Step 2: Configure Network Access

**Allow your IP address to connect:**

1. In Atlas, click **"Network Access"** (left sidebar)
2. Click **"Add IP Address"**
3. Choose one of:
   - **"Add Current IP Address"** (recommended for development)
   - **"Allow Access from Anywhere"** (`0.0.0.0/0`) - for testing only
4. Click **"Confirm"**

⚠️ **Important**: If you're on a dynamic IP (home internet), you may need to update this periodically.

---

### Step 3: Create Database User

1. In Atlas, click **"Database Access"** (left sidebar)
2. Click **"Add New Database User"**
3. Choose **"Password"** authentication
4. Set username and password (save these!)
5. Under **"Database User Privileges"**, select:
   - **"Read and write to any database"** (for demo purposes)
6. Click **"Add User"**

**Example credentials** (you'll use these later):
- Username: `fle_demo_user`
- Password: `YourSecurePassword123!`

---

### Step 4: Get Your Connection String

1. In Atlas, go to your cluster
2. Click **"Connect"**
3. Choose **"Connect your application"**
4. Select **"Java"** and **"5.2.1 or later"**
5. Copy the connection string

**Example connection string**:
```
mongodb+srv://fle_demo_user:<password>@cluster0.abc123.mongodb.net/?retryWrites=true&w=majority
```

---

### Step 5: Configure Application for Atlas

#### Option A: Using `.env` File (Recommended)

Edit the `.env` file in the **root directory** with your Atlas credentials:

```bash
# Edit the root .env file
nano .env
```

Update the MongoDB URI with your Atlas connection string:

```bash
# .env (in root directory)
MONGODB_URI=mongodb+srv://fle_demo_user:YourSecurePassword123!@cluster0.abc123.mongodb.net/?retryWrites=true&w=majority&socketTimeoutMS=60000&connectTimeoutMS=30000&serverSelectionTimeoutMS=30000&maxPoolSize=20&minPoolSize=2
MONGODB_DATABASE=fle_demo
MONGODB_KEYVAULT_NAMESPACE=fle_demo.__keyVault
MONGODB_POOL_MAX_SIZE=20
MONGODB_POOL_MIN_SIZE=2
```

**Important**: Add timeout parameters to avoid socket timeout errors:
- `socketTimeoutMS=60000` - 60 second socket timeout
- `connectTimeoutMS=30000` - 30 second connection timeout
- `serverSelectionTimeoutMS=30000` - 30 second server selection timeout
- `maxPoolSize=20` - Connection pool size
- `minPoolSize=2` - Minimum connections

**Note**: The `.env` file is automatically loaded by Docker Compose and the `run-local.sh` script

#### Option B: Direct Configuration (Not Recommended for Production)

Edit `backend/src/main/resources/application.yml`:

```yaml
mongodb:
  uri: mongodb+srv://fle_demo_user:YourSecurePassword123!@cluster0.abc123.mongodb.net/?retryWrites=true&w=majority&socketTimeoutMS=60000&connectTimeoutMS=30000&serverSelectionTimeoutMS=30000
  database: fle_demo
  keyvault:
    namespace: fle_demo.__keyVault
```

⚠️ **Security Warning**: Never commit credentials to version control!

---

### Step 6: Update Connection Pool Settings for Atlas

Atlas has connection limits based on cluster tier. Update `application.yml`:

```yaml
mongodb:
  connection-pool:
    max-size: 20    # Lower for Atlas (M0 free tier has limits)
    min-size: 2     # Minimum connections
```

**Atlas Connection Limits**:
- **M0 (Free)**: 500 max connections
- **M10**: 1,500 max connections
- **M20+**: 3,000+ max connections

With 50 cached clients × 20 max connections = 1,000 total (fits M10+)

---

### Step 7: Run the Application

#### Option A: Using the Startup Script (Easiest)

The startup script automatically loads the `.env` file:

```bash
cd backend
./run-local.sh
```

#### Option B: Manual Environment Variable Loading

```bash
cd backend

# Load .env file from root directory
export $(cat ../.env | grep -v '^#' | xargs)

# Run the application
mvn spring-boot:run
```

#### Option C: Direct Environment Variable

```bash
cd backend

# Set the variable directly
export MONGODB_URI="mongodb+srv://user:pass@cluster.mongodb.net/..."

# Run the application
mvn spring-boot:run
```

**Look for these success messages in the logs:**
- ✅ "Master key initialized for tenant: tenant_alpha"
- ✅ "Creating new DEK for tenant 'tenant_alpha'"
- ✅ "Seeding data for tenant: tenant_alpha"
- ✅ "Started FledemoApplication"

---

## 🔧 Troubleshooting Atlas Connection Issues

### Issue 1: Socket Timeout Errors

**Error**:
```
com.mongodb.MongoSocketReadTimeoutException: Timeout while receiving message
```

**Solutions**:
1. ✅ Add timeout parameters to connection string (see Step 5)
2. ✅ Check network connectivity: `ping cluster0.abc123.mongodb.net`
3. ✅ Verify IP whitelist in Atlas Network Access
4. ✅ Disable VPN temporarily to test
5. ✅ Check firewall settings (allow outbound port 27017)

### Issue 2: Authentication Failed

**Error**:
```
com.mongodb.MongoSecurityException: Exception authenticating
```

**Solutions**:
1. ✅ Verify username and password are correct
2. ✅ Check that user has proper permissions
3. ✅ Ensure password is URL-encoded (special characters like `@`, `!`, `#`)

**URL Encoding Examples**:
- `@` → `%40`
- `!` → `%21`
- `#` → `%23`

### Issue 3: IP Not Whitelisted

**Error**:
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms while waiting to connect
```

**Solutions**:
1. ✅ Add your current IP to Atlas Network Access
2. ✅ Check if your IP changed (dynamic IP)
3. ✅ Temporarily allow `0.0.0.0/0` for testing

---

## 🎯 Complete Atlas Configuration Example

Here's a complete working configuration:

**.env** (in root directory):
```bash
MONGODB_URI=mongodb+srv://fle_demo_user:SecurePass123%21@mycluster.abc123.mongodb.net/?retryWrites=true&w=majority&socketTimeoutMS=60000&connectTimeoutMS=30000&serverSelectionTimeoutMS=30000&maxPoolSize=20&minPoolSize=2
MONGODB_DATABASE=fle_demo
MONGODB_KEYVAULT_NAMESPACE=fle_demo.__keyVault
MONGODB_POOL_MAX_SIZE=20
MONGODB_POOL_MIN_SIZE=2
```

**backend/src/main/resources/application.yml**:
```yaml
mongodb:
  uri: ${MONGODB_URI:mongodb://localhost:27017}
  database: ${MONGODB_DATABASE:fle_demo}
  keyvault:
    namespace: ${MONGODB_KEYVAULT_NAMESPACE:fle_demo.__keyVault}
  
  connection-pool:
    max-size: 20
    min-size: 2
```

---

## 📊 Verify Setup

After configuration, verify everything works:

```bash
# 1. Start backend
cd backend
mvn spring-boot:run

# 2. Check logs for successful startup
# Should see: "Started FledemoApplication in X.XXX seconds"

# 3. Test API
curl http://localhost:8080/api/v1/tenants

# Expected response:
# ["tenant_alpha","tenant_beta","tenant_gamma"]

# 4. Check MongoDB Atlas
# - Go to Atlas Console
# - Click "Browse Collections"
# - Verify databases: fle_demo
# - Verify collections: customers, orders, __keyVault
```

---

## 🔒 Security Best Practices for Atlas

1. **Never commit credentials**: Use environment variables
2. **Restrict IP access**: Don't use `0.0.0.0/0` in production
3. **Use strong passwords**: 16+ characters with mixed case, numbers, symbols
4. **Enable audit logs**: Available on M10+ clusters
5. **Enable encryption at rest**: Available on M10+ clusters
6. **Rotate credentials**: Change passwords periodically

---

## 🚀 Next Steps

Once Atlas is configured:

1. ✅ Run the backend: `mvn spring-boot:run`
2. ✅ Run the frontend: `cd frontend && npm run dev`
3. ✅ Access the app: http://localhost:3000
4. ✅ Test CSFLE features (DBA view, cross-tenant attack, etc.)

---

## 📚 Additional Resources

- [MongoDB Atlas Documentation](https://www.mongodb.com/docs/atlas/)
- [CSFLE Documentation](https://www.mongodb.com/docs/manual/core/csfle/)
- [Connection String Options](https://www.mongodb.com/docs/manual/reference/connection-string/)
- [Atlas Network Security](https://www.mongodb.com/docs/atlas/security/ip-access-list/)


# MongoDB Atlas Troubleshooting Guide

Quick troubleshooting guide for common MongoDB Atlas connection issues with the CSFLE demo.

---

## 🔴 Socket Timeout Errors

### Error Message:
```
com.mongodb.MongoSocketReadTimeoutException: Timeout while receiving message
Caused by: java.net.SocketTimeoutException: Read timed out
```

### ✅ Solution Checklist:

#### 1. Add Timeout Parameters to Connection String

Your connection string **MUST** include these timeout parameters:

```bash
mongodb+srv://user:pass@cluster.mongodb.net/?retryWrites=true&w=majority&socketTimeoutMS=60000&connectTimeoutMS=30000&serverSelectionTimeoutMS=30000
```

**Key parameters**:
- `socketTimeoutMS=60000` - Socket read timeout (60 seconds)
- `connectTimeoutMS=30000` - Initial connection timeout (30 seconds)
- `serverSelectionTimeoutMS=30000` - Server selection timeout (30 seconds)

#### 2. Verify IP Whitelist in Atlas

1. Go to [MongoDB Atlas Console](https://cloud.mongodb.com)
2. Select your cluster
3. Click **"Network Access"** (left sidebar)
4. Verify your current IP is listed

**Quick test**: Temporarily add `0.0.0.0/0` (allow all IPs):
- Click **"Add IP Address"**
- Click **"Allow Access from Anywhere"**
- Click **"Confirm"**
- Try connecting again

⚠️ **Remove `0.0.0.0/0` after testing!**

#### 3. Test Network Connectivity

```bash
# Test DNS resolution
nslookup your-cluster.mongodb.net

# Test connectivity (macOS/Linux)
nc -zv your-cluster.mongodb.net 27017

# Or use telnet
telnet your-cluster.mongodb.net 27017
```

**If these fail**, the issue is network-related:
- Check firewall settings
- Disable VPN temporarily
- Check corporate network restrictions
- Try from a different network (mobile hotspot)

#### 4. Verify Cluster is Running

1. Go to Atlas Console
2. Check cluster status (should show green/active)
3. Look for maintenance windows or outages

#### 5. Test with MongoDB Compass

Download [MongoDB Compass](https://www.mongodb.com/try/download/compass) and test your connection string:

```
mongodb+srv://user:pass@cluster.mongodb.net/?retryWrites=true&w=majority&socketTimeoutMS=60000&connectTimeoutMS=30000&serverSelectionTimeoutMS=30000
```

If Compass can't connect → Network issue
If Compass connects → Application configuration issue

---

## 🔴 Authentication Errors

### Error Message:
```
com.mongodb.MongoSecurityException: Exception authenticating
```

### ✅ Solution Checklist:

#### 1. Verify Credentials

- Username is correct
- Password is correct
- User exists in Atlas Database Access

#### 2. URL Encode Special Characters

If your password contains special characters, they must be URL-encoded:

| Character | Encoded |
|-----------|---------|
| `@` | `%40` |
| `!` | `%21` |
| `#` | `%23` |
| `$` | `%24` |
| `%` | `%25` |
| `^` | `%5E` |
| `&` | `%26` |
| `*` | `%2A` |

**Example**:
- Password: `MyPass@123!`
- Encoded: `MyPass%40123%21`
- Connection string: `mongodb+srv://user:MyPass%40123%21@cluster.mongodb.net/...`

#### 3. Check User Permissions

1. Go to Atlas → **Database Access**
2. Find your user
3. Verify permissions include:
   - **"Read and write to any database"** (for demo)
   - Or specific permissions for `fle_demo` database

---

## 🔴 Connection Timeout Errors

### Error Message:
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms while waiting to connect
```

### ✅ Solution:

This usually means your IP is not whitelisted. Follow steps in "Socket Timeout Errors" → "Verify IP Whitelist".

---

## 🔴 CSFLE-Specific Errors

### Error: Automatic Encryption Shared Library Not Found

```
mongocrypt_t object is null
```

**Solution**: Install the MongoDB Automatic Encryption Shared Library

**macOS (Intel)**:
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

**macOS (Apple Silicon)**:
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

**Linux**:
```bash
curl -O https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-ubuntu2204-x86_64-enterprise-8.0.3.tgz
tar -xvf mongo_crypt_shared_v1-ubuntu2204-x86_64-enterprise-8.0.3.tgz
sudo cp lib/mongo_crypt_v1.so /usr/local/lib/
```

---

## 📋 Complete Configuration Example for Atlas

Here's a working configuration for Atlas:

### 1. Create `.env` file in `backend/` directory:

```bash
# backend/.env
MONGODB_URI=mongodb+srv://fle_demo_user:YourPassword123%21@cluster0.abc123.mongodb.net/?retryWrites=true&w=majority&socketTimeoutMS=60000&connectTimeoutMS=30000&serverSelectionTimeoutMS=30000&maxPoolSize=20&minPoolSize=2
MONGODB_DATABASE=fle_demo
MONGODB_KEYVAULT_NAMESPACE=fle_demo.__keyVault
MONGODB_POOL_MAX_SIZE=20
MONGODB_POOL_MIN_SIZE=2
```

### 2. Verify `application.yml` uses environment variables:

```yaml
mongodb:
  uri: ${MONGODB_URI:mongodb://mongodb:27017}
  database: ${MONGODB_DATABASE:fle_demo}
  keyvault:
    namespace: ${MONGODB_KEYVAULT_NAMESPACE:fle_demo.__keyVault}
  connection-pool:
    max-size: ${MONGODB_POOL_MAX_SIZE:20}
    min-size: ${MONGODB_POOL_MIN_SIZE:2}
```

### 3. Run the application:

```bash
cd backend
mvn spring-boot:run
```

---

## 🧪 Quick Diagnostic Commands

Run these to diagnose your issue:

```bash
# 1. Test DNS resolution
nslookup your-cluster.mongodb.net

# 2. Test connectivity
nc -zv your-cluster.mongodb.net 27017

# 3. Check if library is installed
ls -la /usr/local/lib/mongo_crypt_v1.*

# 4. Test with MongoDB Compass
# Download from: https://www.mongodb.com/try/download/compass
# Use your connection string

# 5. Check Java version
java -version  # Should be 21+

# 6. Check Maven version
mvn -version   # Should be 3.6+
```

---

## 📞 Still Having Issues?

If you've tried everything above and still can't connect:

1. **Check Atlas Status**: https://status.mongodb.com/
2. **Review Atlas Logs**: Atlas Console → Cluster → Metrics → Logs
3. **Try Different Network**: Use mobile hotspot to rule out network issues
4. **Contact Atlas Support**: If using paid tier (M10+)

---

## ✅ Success Indicators

When everything is working, you should see:

```
2026-03-XX XX:XX:XX.XXX  INFO --- [main] c.e.fledemo.config.LocalKmsProvider      : Master key initialized for tenant: tenant_alpha
2026-03-XX XX:XX:XX.XXX  INFO --- [main] c.e.fledemo.config.LocalKmsProvider      : Master key initialized for tenant: tenant_beta
2026-03-XX XX:XX:XX.XXX  INFO --- [main] c.e.fledemo.config.LocalKmsProvider      : Master key initialized for tenant: tenant_gamma
2026-03-XX XX:XX:XX.XXX  INFO --- [main] c.e.fledemo.keyvault.TenantKeyService    : Creating new DEK for tenant 'tenant_alpha'...
2026-03-XX XX:XX:XX.XXX  INFO --- [main] c.e.fledemo.service.DataSeeder           : Seeding data for tenant: tenant_alpha
2026-03-XX XX:XX:XX.XXX  INFO --- [main] c.e.fledemo.FledemoApplication           : Started FledemoApplication in X.XXX seconds
```

**No timeout errors!** ✅


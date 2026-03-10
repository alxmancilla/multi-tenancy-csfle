# Troubleshooting Guide

Common issues and solutions for the MongoDB CSFLE Multi-Tenancy Demo.

## 🐳 Docker Issues

### Issue: ClassNotFoundException for MongoDB Driver Classes

**Error:**
```
java.lang.ClassNotFoundException: com.mongodb.internal.connection.DefaultServerMonitor$1
```

**Cause:** 
- Corrupted Maven dependencies in Docker build cache
- Version conflict between Spring Boot managed dependencies and explicit MongoDB driver versions

**Solution 1: Clean Docker Build**
```bash
# Remove all containers, images, and build cache
docker-compose down -v --rmi all
docker builder prune -a -f

# Rebuild from scratch
docker-compose up --build
```

**Solution 2: Force Maven Dependency Resolution**
```bash
# If Solution 1 doesn't work, rebuild with no cache
docker-compose build --no-cache
docker-compose up
```

**Solution 3: Verify pom.xml has dependencyManagement**
The `backend/pom.xml` should have a `<dependencyManagement>` section that overrides Spring Boot's managed MongoDB versions. This was added to fix version conflicts.

---

### Issue: mongo_crypt_v1.so Not Found

**Error:**
```
mongo_crypt_v1.so: cannot open shared object file: No such file or directory
```

**Cause:** 
- Wrong architecture version downloaded (ARM64 vs x86_64)
- Library not in the correct path

**Solution:**
The Dockerfile automatically detects architecture and downloads the correct version. If this fails:

```bash
# Check what architecture Docker is using
docker run --rm eclipse-temurin:21-jre uname -m

# Rebuild with verbose output
docker-compose build --progress=plain backend

# Look for the line: "Downloading ARM64 version..." or "Downloading x86_64 version..."
```

---

### Issue: Backend Container Keeps Restarting

**Error:**
```
fle-demo-backend exited with code 1
```

**Solution:**
```bash
# Check backend logs
docker-compose logs backend

# Common causes:
# 1. MongoDB not ready - wait for health check
# 2. Missing master key files - check backend/ directory
# 3. Wrong MONGODB_URI in .env file

# Verify master key files exist
ls -la backend/local_master_key_tenant_*.bin

# Check MongoDB is healthy
docker-compose ps
# mongodb should show "healthy"
```

---

### Issue: Port Already in Use

**Error:**
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**Solution:**
```bash
# Find what's using the port
lsof -i :8080
lsof -i :3000
lsof -i :27017

# Kill the process
kill -9 <PID>

# Or change ports in docker-compose.yml
```

---

## 💻 Local Installation Issues

### Issue: Could Not Find Crypt Shared Library

**Error:**
```
Could not find crypt_shared library
```

**Solution:**
Download and install the MongoDB Automatic Encryption Shared Library:

**macOS (Intel):**
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.5.tgz
tar -xvf mongo_crypt_shared_v1-macos-x86_64-enterprise-8.0.5.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

**macOS (Apple Silicon):**
```bash
curl -O https://downloads.mongodb.com/osx/mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.5.tgz
tar -xvf mongo_crypt_shared_v1-macos-arm64-enterprise-8.0.5.tgz
sudo cp lib/mongo_crypt_v1.dylib /usr/local/lib/
```

**Linux (x86_64):**
```bash
curl -O https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-linux-x86_64-enterprise-ubuntu2204-8.0.5.tgz
tar -xvf mongo_crypt_shared_v1-linux-x86_64-enterprise-ubuntu2204-8.0.5.tgz
sudo cp lib/mongo_crypt_v1.so /usr/local/lib/
```

**Linux (ARM64):**
```bash
curl -O https://downloads.mongodb.com/linux/mongo_crypt_shared_v1-linux-aarch64-enterprise-ubuntu2204-8.0.5.tgz
tar -xvf mongo_crypt_shared_v1-linux-aarch64-enterprise-ubuntu2204-8.0.5.tgz
sudo cp lib/mongo_crypt_v1.so /usr/local/lib/
```

---

### Issue: Java Version Mismatch

**Error:**
```
Unsupported class file major version 65
```

**Cause:** Using Java version older than 21

**Solution:**
```bash
# Check Java version
java -version

# Should show: openjdk version "21.x.x" or higher

# Install Java 21
# macOS:
brew install openjdk@21

# Ubuntu:
sudo apt install openjdk-21-jdk

# Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux
```

---

## 🔐 Encryption Issues

### Issue: Master Key File Not Found

**Error:**
```
FileNotFoundException: local_master_key_tenant_alpha.bin
```

**Solution:**
Master key files are auto-generated on first run. If missing:

```bash
cd backend

# The application will create these files automatically on first run
# Just make sure the backend/ directory is writable

# Verify files were created
ls -la local_master_key_tenant_*.bin

# Each file should be exactly 96 bytes
```

---

### Issue: Cannot Decrypt Data

**Error:**
```
MongoException: HMAC validation failure
```

**Cause:** Using wrong master key for tenant

**Solution:**
This is expected behavior! Each tenant's data can only be decrypted with their own master key. This demonstrates cryptographic isolation.

If you deleted master key files and regenerated them, old encrypted data cannot be decrypted.

**To reset:**
```bash
# Stop backend
# Delete master key files
rm backend/local_master_key_tenant_*.bin

# Delete MongoDB data
mongo fle_demo --eval "db.dropDatabase()"

# Restart backend - new keys will be generated
```

---

## 🌐 Network/Connection Issues

### Issue: Frontend Cannot Connect to Backend

**Error in browser console:**
```
Network Error: Failed to fetch
```

**Solution:**
```bash
# Check backend is running
curl http://localhost:8080/api/v1/tenants

# Check CORS configuration
# Backend should allow requests from http://localhost:5173 (Vite dev server)

# Verify VITE_API_BASE_URL in frontend
# Should be: http://localhost:8080
```

---

### Issue: Cannot Connect to MongoDB

**Error:**
```
MongoTimeoutException: Timed out after 30000 ms
```

**Solution:**
```bash
# Check MongoDB is running
mongosh --eval "db.adminCommand('ping')"

# Check connection string in .env or application.yml
# Local: mongodb://localhost:27017
# Atlas: mongodb+srv://...

# For Docker: use service name 'mongodb' not 'localhost'
MONGODB_URI=mongodb://mongodb:27017
```

---

## 📚 Additional Resources

- [DOCKER_SETUP.md](DOCKER_SETUP.md) - Docker setup guide
- [GETTING_STARTED.md](GETTING_STARTED.md) - Quick start guide
- [README.md](README.md) - Complete documentation
- [MongoDB CSFLE Docs](https://docs.mongodb.com/manual/core/csfle/)


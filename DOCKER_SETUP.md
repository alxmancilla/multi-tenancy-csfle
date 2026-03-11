# Docker Setup Guide

This guide explains how to run the MongoDB CSFLE Multi-Tenancy Demo using Docker containers.

## 🐳 Prerequisites

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher

Check your versions:
```bash
docker --version
docker-compose --version
```

## 🚀 Quick Start

### Step 1: Configure Environment Variables

Docker Compose automatically loads variables from the `.env` file:

```bash
# Copy the example environment file
cp .env.example .env

# (Optional) Edit .env to customize configuration
# For MongoDB Atlas: Update MONGODB_URI in .env
# For local MongoDB: Use the default settings
```

### Step 2: Start the Application

**Option A: Use Local MongoDB Container (Default)**
```bash
docker-compose up --build
```

**Option B: Use MongoDB Atlas**
```bash
# 1. Edit .env and set your MongoDB Atlas URI:
#    MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/...
#
# 2. Comment out the 'mongodb' service in docker-compose.yml
#
# 3. Start services:
docker-compose up --build
```

This will:
1. Load environment variables from `.env` file
2. Build the backend Docker image (Java 21 + Spring Boot)
3. Build the frontend Docker image (React + Nginx)
4. Start MongoDB 8.0 container (if using local MongoDB)
5. Start all services with proper networking and health checks

**Access the application:**
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **MongoDB**: localhost:27017 (if using local container)

## 📦 What's Included

### Services

1. **MongoDB** (`mongodb`)
   - Image: `mongo:8.0`
   - Port: `27017`
   - Database: `fle_demo`
   - Persistent volume for data

2. **Backend** (`backend`)
   - Java 21 + Spring Boot
   - MongoDB CSFLE support
   - Automatic Encryption Shared Library included
   - Port: `8080`

3. **Frontend** (`frontend`)
   - React 18 + Vite
   - Nginx for production serving
   - Port: `3000` (mapped to nginx port 80)

### Volumes

- `mongodb_data`: Persistent storage for MongoDB data
- `master_keys`: Persistent storage for tenant master key files
  - **Why a named volume?** Docker creates directories when bind-mounting non-existent files, which breaks the application. A named volume allows the app to generate key files on first run.
  - **Location in container:** `/app/keys/`
  - **Files:** `local_master_key_tenant_alpha.bin`, `local_master_key_tenant_beta.bin`, `local_master_key_tenant_gamma.bin`

### Network

- `fle-network`: Bridge network connecting all services

### MongoDB Automatic Encryption Shared Library

The backend container automatically downloads and configures the **MongoDB Automatic Encryption Shared Library** (`mongo_crypt_v1.so`):

- **Purpose**: Enables Client-Side Field Level Encryption (CSFLE) without requiring `mongocryptd`
- **Architecture Detection**: Automatically detects CPU architecture and downloads the correct version:
  - **ARM64/aarch64** (Apple Silicon M1/M2/M3): Downloads ARM64 version
  - **x86_64** (Intel/AMD): Downloads x86_64 version
- **Version**: 8.0.5 (compatible with MongoDB Driver 5.2.1)
- **Location**: `/usr/local/lib/mongo_crypt_v1.so`
- **Environment Variables**:
  - `MONGOCRYPT_SHARED_LIB_PATH=/usr/local/lib/mongo_crypt_v1.so`
  - `LD_LIBRARY_PATH=/usr/local/lib`

This library is the **modern replacement for mongocryptd** and provides better performance and easier deployment.

## 🔧 Docker Commands

### Start the application
```bash
# Start in foreground (see logs)
docker-compose up

# Start in background (detached mode)
docker-compose up -d

# Rebuild and start
docker-compose up --build
```

### Stop the application
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mongodb
```

### Check service status
```bash
docker-compose ps
```

### Restart a specific service
```bash
docker-compose restart backend
docker-compose restart frontend
```

### Execute commands in containers
```bash
# Access MongoDB shell
docker-compose exec mongodb mongosh fle_demo

# Access backend container
docker-compose exec backend sh

# Access frontend container
docker-compose exec frontend sh
```

## 🔍 Troubleshooting

### Port conflicts
If ports 3000, 8080, or 27017 are already in use:

**Option 1**: Stop the conflicting service
```bash
# Find what's using the port
lsof -i :8080
lsof -i :3000
lsof -i :27017

# Kill the process
kill -9 <PID>
```

**Option 2**: Change ports in `docker-compose.yml`
```yaml
services:
  frontend:
    ports:
      - "3001:80"  # Change 3000 to 3001
  backend:
    ports:
      - "8081:8080"  # Change 8080 to 8081
```

### Backend fails to start
```bash
# Check backend logs
docker-compose logs backend

# Common issues:
# 1. MongoDB not ready - wait for health check
# 2. ClassNotFoundException - see TROUBLESHOOTING.md
# 3. Master key path is a directory - fixed by using named volume

# For detailed solutions, see TROUBLESHOOTING.md
```

### Frontend can't connect to backend
```bash
# Verify backend is healthy
curl http://localhost:8080/api/v1/tenants

# Check network connectivity
docker-compose exec frontend ping backend
```

### Clean restart
```bash
# Stop everything and remove volumes
docker-compose down -v

# Remove all images
docker-compose down --rmi all

# Rebuild from scratch
docker-compose up --build
```

## 🔐 Master Keys

The Docker setup uses a **named volume** (`master_keys`) to store tenant master key files:
- `local_master_key_tenant_alpha.bin`
- `local_master_key_tenant_beta.bin`
- `local_master_key_tenant_gamma.bin`

### How It Works

1. **First Run**: The application automatically generates 96-byte random master keys for each tenant and saves them to `/app/keys/` in the container
2. **Subsequent Runs**: The application loads existing keys from the Docker volume
3. **Persistence**: Keys are stored in a Docker named volume and persist across container restarts

### Why Not Bind Mounts?

Previously, the setup used bind mounts like:
```yaml
- ./backend/local_master_key_tenant_alpha.bin:/app/local_master_key_tenant_alpha.bin
```

**Problem**: When the file doesn't exist on the host, Docker creates a **directory** instead, causing the application to crash with `FileNotFoundException` or `IllegalStateException`.

**Solution**: Use a named volume that allows the application to create files on first run.

### Managing Master Keys

**View keys in the volume:**
```bash
# Inspect the volume
docker volume inspect multi-tenancy-csfle_master_keys

# Access keys from a running container
docker-compose exec backend ls -la /app/keys/
```

**Reset keys (regenerate):**
```bash
# WARNING: This will make existing encrypted data unreadable!
docker-compose down -v
docker volume rm multi-tenancy-csfle_master_keys
docker-compose up --build
```

**Backup keys:**
```bash
# Create a temporary container to copy keys out
docker run --rm -v multi-tenancy-csfle_master_keys:/keys -v $(pwd):/backup alpine cp -r /keys /backup/master_keys_backup
```

**Restore keys:**
```bash
# Copy keys back into the volume
docker run --rm -v multi-tenancy-csfle_master_keys:/keys -v $(pwd)/master_keys_backup:/backup alpine cp -r /backup/. /keys/
```

## 🌐 Environment Variables

Docker Compose automatically loads environment variables from the `.env` file in the project root.

### Configuration File: `.env`

Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
```

### Available Variables

**Backend:**
- `MONGODB_URI`: MongoDB connection string (default: `mongodb://mongodb:27017`)
  - For local: `mongodb://mongodb:27017`
  - For Atlas: `mongodb+srv://username:password@cluster.mongodb.net/...`
- `MONGODB_DATABASE`: Database name (default: `fle_demo`)
- `MONGODB_KEYVAULT_NAMESPACE`: Key vault namespace (default: `fle_demo.__keyVault`)
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: `docker`)

**Frontend:**
- `VITE_API_BASE_URL`: Backend API URL (default: `http://localhost:8080`)

### Using MongoDB Atlas

To use MongoDB Atlas instead of the local container:

1. Edit `.env` and set your Atlas connection string:
   ```bash
   MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/?retryWrites=true&w=majority
   ```

2. Comment out the `mongodb` service in `docker-compose.yml` (lines 12-28)

3. Start services:
   ```bash
   docker-compose up --build
   ```

## 📊 Health Checks

All services include health checks:

**MongoDB**: Checks database connectivity
```bash
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"
```

**Backend**: Checks API endpoint
```bash
curl http://localhost:8080/api/v1/tenants
```

## 🎯 Production Considerations

This Docker setup is designed for **demonstration purposes**. For production:

1. **Use external MongoDB** (MongoDB Atlas recommended)
   - Update `MONGODB_URI` in docker-compose.yml
   - Remove the mongodb service

2. **Use enterprise KMS** instead of local master keys
   - AWS KMS, Azure Key Vault, or GCP KMS
   - Update backend configuration

3. **Enable HTTPS**
   - Add SSL certificates
   - Configure nginx for HTTPS

4. **Use Docker secrets** for sensitive data
   - Master keys
   - MongoDB credentials

5. **Add monitoring and logging**
   - Prometheus + Grafana
   - ELK stack

## 📚 Next Steps

- Read [README.md](README.md) for application documentation
- Read [GETTING_STARTED.md](GETTING_STARTED.md) for feature walkthrough
- Read [PRESENTATION.md](PRESENTATION.md) for technical deep dive


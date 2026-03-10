# Docker Quick Reference

Quick command reference for running the MongoDB CSFLE Multi-Tenancy Demo with Docker.

## 🚀 Quick Start

```bash
# Copy environment file
cp .env.example .env

# (Optional) Edit .env to customize configuration
# For MongoDB Atlas: Update MONGODB_URI in .env

# Start everything
docker-compose up --build

# Or use the helper script
./docker-start.sh
```

**Note:** Docker Compose automatically loads environment variables from the `.env` file.

## 📋 Common Commands

### Starting Services

```bash
# Start in foreground (see logs)
docker-compose up

# Start in background (detached)
docker-compose up -d

# Rebuild and start
docker-compose up --build

# Start specific service
docker-compose up backend
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Stop and remove images
docker-compose down --rmi all
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mongodb

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Service Status

```bash
# Check status
docker-compose ps

# Check resource usage
docker stats
```

### Restarting Services

```bash
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart backend
docker-compose restart frontend
```

### Accessing Containers

```bash
# MongoDB shell
docker-compose exec mongodb mongosh fle_demo

# Backend shell
docker-compose exec backend sh

# Frontend shell
docker-compose exec frontend sh
```

### Cleaning Up

```bash
# Remove stopped containers
docker-compose rm

# Remove all (containers, networks, volumes)
docker-compose down -v

# Remove unused Docker resources
docker system prune -a
```

## 🔍 Debugging

### Check Backend Health

```bash
# From host
curl http://localhost:8080/api/v1/tenants

# From container
docker-compose exec backend curl http://localhost:8080/api/v1/tenants
```

### Check MongoDB

```bash
# Connect to MongoDB
docker-compose exec mongodb mongosh fle_demo

# In mongosh:
show collections
db.customers.countDocuments()
db.__keyVault.countDocuments()
```

### View Environment Variables

```bash
# Backend
docker-compose exec backend env | grep MONGODB

# Frontend
docker-compose exec frontend env | grep VITE
```

### Check Network Connectivity

```bash
# Frontend to Backend
docker-compose exec frontend ping backend

# Backend to MongoDB
docker-compose exec backend ping mongodb
```

## 🛠️ Development Workflow

### Rebuild After Code Changes

```bash
# Backend changes
docker-compose up --build backend

# Frontend changes
docker-compose up --build frontend

# Both
docker-compose up --build
```

### View Real-time Logs During Development

```bash
# Terminal 1: Start services
docker-compose up

# Terminal 2: Watch specific logs
docker-compose logs -f backend
```

### Reset Database

```bash
# Stop and remove volumes
docker-compose down -v

# Start fresh
docker-compose up
```

## 📊 Monitoring

### Resource Usage

```bash
# All containers
docker stats

# Specific container
docker stats fle-demo-backend
```

### Disk Usage

```bash
# Docker disk usage
docker system df

# Detailed view
docker system df -v
```

## 🔧 Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :8080
lsof -i :3000
lsof -i :27017

# Kill process
kill -9 <PID>
```

### Container Won't Start

```bash
# Check logs
docker-compose logs backend

# Inspect container
docker inspect fle-demo-backend

# Check health
docker-compose ps
```

### Clean Restart

```bash
# Nuclear option - remove everything
docker-compose down -v --rmi all
docker system prune -a

# Rebuild from scratch
docker-compose up --build
```

## 📚 More Information

- Full Docker guide: [DOCKER_SETUP.md](DOCKER_SETUP.md)
- Application docs: [README.md](README.md)
- Getting started: [GETTING_STARTED.md](GETTING_STARTED.md)


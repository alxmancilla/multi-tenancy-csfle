#!/bin/bash

# MongoDB CSFLE Multi-Tenancy Demo - Docker Quick Start Script
# This script starts the entire application stack using Docker Compose

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  MongoDB CSFLE Multi-Tenancy Demo - Docker Quick Start    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    echo "   Visit: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    echo "   Visit: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✅ Docker is installed${NC}"
echo -e "${GREEN}✅ Docker Compose is installed${NC}"
echo ""

# Check if master key files exist
echo -e "${YELLOW}🔍 Checking master key files...${NC}"
KEYS_MISSING=0

if [ ! -f "backend/local_master_key_tenant_alpha.bin" ]; then
    echo -e "${RED}❌ Missing: backend/local_master_key_tenant_alpha.bin${NC}"
    KEYS_MISSING=1
fi

if [ ! -f "backend/local_master_key_tenant_beta.bin" ]; then
    echo -e "${RED}❌ Missing: backend/local_master_key_tenant_beta.bin${NC}"
    KEYS_MISSING=1
fi

if [ ! -f "backend/local_master_key_tenant_gamma.bin" ]; then
    echo -e "${RED}❌ Missing: backend/local_master_key_tenant_gamma.bin${NC}"
    KEYS_MISSING=1
fi

if [ $KEYS_MISSING -eq 1 ]; then
    echo -e "${RED}❌ Master key files are missing. These files are required for CSFLE.${NC}"
    echo -e "${YELLOW}   The backend will generate them on first run.${NC}"
    echo ""
fi

echo -e "${GREEN}✅ Master key files check complete${NC}"
echo ""

# Check for port conflicts
echo -e "${YELLOW}🔍 Checking for port conflicts...${NC}"
PORTS_IN_USE=0

if lsof -Pi :3000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port 3000 is already in use (Frontend)${NC}"
    PORTS_IN_USE=1
fi

if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port 8080 is already in use (Backend)${NC}"
    PORTS_IN_USE=1
fi

if lsof -Pi :27017 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Port 27017 is already in use (MongoDB)${NC}"
    PORTS_IN_USE=1
fi

if [ $PORTS_IN_USE -eq 1 ]; then
    echo -e "${YELLOW}⚠️  Some ports are in use. Docker Compose may fail to start.${NC}"
    echo -e "${YELLOW}   You can stop the conflicting services or modify docker-compose.yml${NC}"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}✅ All ports are available${NC}"
fi

echo ""
echo -e "${BLUE}🚀 Starting Docker Compose...${NC}"
echo ""

# Start Docker Compose
docker-compose up --build

echo ""
echo -e "${GREEN}✅ Application stopped${NC}"
echo -e "${YELLOW}💡 To start in background mode, use: docker-compose up -d${NC}"
echo -e "${YELLOW}💡 To stop and clean up, use: docker-compose down -v${NC}"


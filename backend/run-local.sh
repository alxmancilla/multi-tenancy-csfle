#!/bin/bash

# MongoDB CSFLE Demo - Local Startup Script
# This script loads environment variables from .env and starts the application

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}MongoDB CSFLE Multi-Tenancy Demo - Local Startup${NC}"
echo "=================================================="
echo ""

# Check for .env file in root directory (preferred) or backend directory
if [ -f ../.env ]; then
    echo -e "${GREEN}✓${NC} Found .env file in root directory, loading environment variables..."

    # Load environment variables from root .env (properly handle special characters)
    set -a  # Automatically export all variables
    source ../.env
    set +a  # Stop automatically exporting

    echo -e "${GREEN}✓${NC} Environment variables loaded from ../.env"
    echo ""
    echo "Configuration:"
    echo "  MONGODB_URI: ${MONGODB_URI:0:50}..." # Show first 50 chars only
    echo "  MONGODB_DATABASE: ${MONGODB_DATABASE}"
    echo "  MONGODB_POOL_MAX_SIZE: ${MONGODB_POOL_MAX_SIZE:-20}"
    echo ""
elif [ -f .env ]; then
    echo -e "${GREEN}✓${NC} Found .env file in backend directory, loading environment variables..."

    # Load environment variables from backend .env (properly handle special characters)
    set -a  # Automatically export all variables
    source .env
    set +a  # Stop automatically exporting

    echo -e "${GREEN}✓${NC} Environment variables loaded from ./backend/.env"
    echo ""
    echo "Configuration:"
    echo "  MONGODB_URI: ${MONGODB_URI:0:50}..." # Show first 50 chars only
    echo "  MONGODB_DATABASE: ${MONGODB_DATABASE}"
    echo "  MONGODB_POOL_MAX_SIZE: ${MONGODB_POOL_MAX_SIZE:-20}"
    echo ""
else
    echo -e "${YELLOW}⚠${NC}  No .env file found in root or backend directory"
    echo ""
    echo "Using default configuration from application.yml"
    echo "  MONGODB_URI: ${MONGODB_URI:-mongodb://localhost:27017}"
    echo ""
fi

# Check if MongoDB URI is set
if [ -z "$MONGODB_URI" ]; then
    echo -e "${YELLOW}⚠${NC}  MONGODB_URI not set, using default: mongodb://localhost:27017"
    echo ""
fi

echo "Starting application..."
echo "=================================================="
echo ""

# Run the application
mvn spring-boot:run


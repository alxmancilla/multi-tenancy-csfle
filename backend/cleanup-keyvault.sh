#!/bin/bash

# Cleanup script to remove old DEKs from the key vault
# This is needed when changing the KMS provider configuration

echo "🧹 Cleaning up MongoDB Key Vault..."
echo ""
echo "This script will:"
echo "1. Connect to your MongoDB Atlas cluster"
echo "2. Drop the __keyVault collection"
echo "3. Allow the application to create fresh DEKs with the new configuration"
echo ""
read -p "Press Enter to continue or Ctrl+C to cancel..."

# MongoDB connection string
MONGO_URI="mongodb+srv://demo:d3m0p4ss@democluster.elz1q.mongodb.net/fle_demo"

# Drop the key vault collection
mongosh "$MONGO_URI" --eval "db.__keyVault.drop()" --quiet

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Key vault cleaned successfully!"
    echo ""
    echo "Next steps:"
    echo "1. Delete local master key files: rm -f local_master_key*.bin"
    echo "2. Restart the application: mvn spring-boot:run"
    echo ""
else
    echo ""
    echo "❌ Failed to clean key vault. Please check your MongoDB connection."
    echo ""
    echo "Alternative: Use MongoDB Compass or Atlas UI to manually delete the 'fle_demo.__keyVault' collection"
    echo ""
fi


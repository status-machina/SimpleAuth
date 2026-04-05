#!/bin/bash
set -e

echo "🏗️  Building SimpleAuth Fabric mod..."
JAVA_HOME="/Users/dallashall/Library/Application Support/PrismLauncher/java/java-runtime-epsilon" ./gradlew build --no-daemon

echo ""
echo "🐳 Starting Minecraft Fabric 26.1 test server..."
docker-compose up -d

echo ""
echo "✅ Server starting! Logs:"
echo ""
docker-compose logs -f mc

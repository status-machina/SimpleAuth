#!/bin/bash
set -e

echo "🏗️  Building SimpleAuth..."
./gradlew build

echo ""
echo "🐳 Starting production Minecraft server..."
echo "   - Fabric 26.1.1 with SimpleAuth"
echo "   - Geyser + Floodgate (Bedrock support)"
echo ""

docker-compose -f docker-compose.prod.yml up -d

echo ""
echo "✅ Server starting!"
echo ""
echo "Java players:    localhost:25565"
echo "Bedrock players: localhost:19132"
echo ""
echo "To view logs:  docker-compose -f docker-compose.prod.yml logs -f mc"
echo "To access console: docker exec -it mc-server rcon-cli"
echo ""

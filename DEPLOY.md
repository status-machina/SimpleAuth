# Deployment Guide

## 1. Create GitHub Repository

```bash
# Initialize git (if not already done)
git init
git add .
git commit -m "Initial commit"

# Create repo on GitHub, then:
git remote add origin https://github.com/YOURUSER/SimpleAuth.git
git branch -M main
git push -u origin main
```

## 2. Create First Release

```bash
# Tag the release
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will automatically:
- Build the mod with Gradle
- Create a GitHub release
- Upload `simpleauth-1.0.0.jar` as a release asset

## 3. Update docker-compose.prod.yml

Replace `YOURUSER` in the MODS URL:
```yaml
MODS: |
  https://github.com/statusmachina/SimpleAuth/releases/latest/download/simpleauth-1.0.0.jar
  ...
```

## 4. Deploy to Remote Server

```bash
# Copy docker-compose.prod.yml to your server
scp docker-compose.prod.yml user@your-server:~/minecraft/

# SSH to server
ssh user@your-server

# Start the server
cd ~/minecraft
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f mc
```

## 5. Server Management

### View Console
```bash
docker exec -it mc-server rcon-cli
```

### Set Player Passwords
```bash
# In RCON console
setpassword Dad password123
setpassword Nessa secret456
```

### Server Commands
```bash
# View logs
docker-compose -f docker-compose.prod.yml logs -f mc

# Restart server
docker-compose -f docker-compose.prod.yml restart mc

# Stop server (keep data)
docker-compose -f docker-compose.prod.yml down

# Stop and wipe all data
docker-compose -f docker-compose.prod.yml down -v
```

## 6. Making Updates

```bash
# Make code changes, commit them
git add .
git commit -m "Fix authentication bug"
git push

# Create new release
git tag v1.0.1
git push origin v1.0.1

# On server - pull new version
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

The server will automatically download the latest release!

## Server Ports

- **25565** - Java Edition (TCP)
- **19132** - Bedrock Edition (UDP)

Make sure these ports are open in your firewall.

## Player Connection

**Java Edition:**
- Server address: `your-server-ip:25565`
- Version: 26.1.1

**Bedrock Edition:**
- Server address: `your-server-ip`
- Port: `19132`

Both will require authentication via `/login <password>` after an admin sets their password.

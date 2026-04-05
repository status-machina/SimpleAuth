# Testing SimpleAuth (Fabric 26.1)

## Quick Start

```bash
# 1. Build and start server
./test-server.sh

# 2. Wait for server to start (look for "Done!")
# Press Ctrl+C to exit logs (server keeps running)

# 3. Connect to console
docker exec -it mc-simpleauth-fabric rcon-cli

# 4. Set a password for a player
setpassword Dad mypassword123

# 5. Exit console (Ctrl+D)
```

## Test Scenarios

### Scenario 1: First-Time Player (No Password)

1. **Join as `Dad`** (whitelisted)
2. **Expected behavior:**
   - Player is frozen (can't move)
   - Player is invulnerable and flying
   - Chat message: "First time here! Ask an admin to set your password!"
3. **From console:** `setpassword Dad secret123`
4. **In-game:** `/login secret123`
5. **Expected behavior:**
   - Player unfrozen
   - Can move and play normally
   - Success message shown

### Scenario 2: Returning Player (Has Password)

1. **Join as `Dad`** (already has password from Scenario 1)
2. **Expected behavior:**
   - Player is frozen
   - Chat message: "Welcome back! Please login: /login <password>"
3. **In-game:** `/login secret123`
4. **Expected behavior:**
   - Player authenticated
   - Can play normally

### Scenario 3: Wrong Password

1. **Join as authenticated player**
2. **Log out and rejoin**
3. **Try:** `/login wrongpassword`
4. **Expected behavior:**
   - Error message: "✗ Invalid password!"
   - Player remains frozen

### Scenario 4: Console-Only Password Setting

1. **From console:** `setpassword Nessa password456` ✅ Works
2. **In-game as Op:** `/setpassword Nessa newpass` ❌ Rejected
3. **Expected:** "This command can only be run from console!"

### Scenario 5: Bedrock Player (via Geyser)

1. **Join from Bedrock** (iOS/Android/Console)
2. **Expected behavior:**
   - Player frozen (same as Java)
   - Auth message shown
   - Must type `/login <password>` on mobile

## Useful Docker Commands

```bash
# View live logs
docker-compose logs -f mc

# Access console (RCON)
docker exec -it mc-simpleauth-fabric rcon-cli

# Restart server
docker-compose restart mc

# Stop server (keep data)
docker-compose down

# Stop and wipe everything
docker-compose down -v

# Check mod loaded
docker exec mc-simpleauth-fabric rcon-cli mods

# View database
docker exec mc-simpleauth-fabric ls -lh /data/config/simpleauth/
```

## Database Inspection

```bash
# Copy database from container
docker cp mc-simpleauth-fabric:/data/config/simpleauth/auth.db ./auth.db

# View with sqlite3
sqlite3 auth.db "SELECT username, last_login FROM players;"
```

## Fabric 26.1 Specifics

- **No obfuscation** - Code uses Mojang mappings directly
- **Faster startup** - No remapping needed
- **Java 25** - Modern LTS features available
- **Fabric API 0.145.3+26.1.1** - Latest for 26.1

## Troubleshooting

### Mod not loading?

```bash
# Check if jar is mounted
docker exec mc-simpleauth-fabric ls -lh /data/mods/

# Check server logs for errors
docker-compose logs mc | grep -i "simpleauth\|error"
```

### Can't connect?

```bash
# Verify server is running
docker ps | grep simpleauth-fabric

# Check port binding
docker port mc-simpleauth-fabric
```

### Minecraft 26.1 not available?

If Minecraft 26.1 isn't released yet, the Docker image will fail. Check [Fabric's version support](https://fabricmc.net/use/server/).

## Expected Console Output

When starting, you should see:

```
[SimpleAuth] Initializing SimpleAuth...
[SimpleAuth] SimpleAuth initialized! Players will be locked until authenticated.
```

When player joins:

```
[SimpleAuth] Dad joined - locking player until authenticated
```

When password is set from console:

```
✓ Password set for Dad
```

When player logs in successfully:

```
[SimpleAuth] Dad authenticated successfully
```

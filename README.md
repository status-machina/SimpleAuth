# SimpleAuth

Dead-simple CLI-based authentication mod for Fabric on Minecraft 26.1 ("Tiny Takeover").

## Features

- **Console-only password management** - Admins set passwords via `/setpassword` from console
- **Player lock system** - Unauthenticated players are frozen (no movement, no commands except `/login`)
- **BCrypt password hashing** - Industry-standard secure password storage
- **SQLite database** - Lightweight, file-based storage
- **Geyser + Floodgate** - Full Bedrock player support

## Requirements

- **Java 25+** (required for Minecraft 26.1)
- **Fabric Server** with Minecraft 26.1
- **Fabric Loader 0.18.6+**
- **Fabric API 0.145.3+26.1.1**

## Building

```bash
JAVA_HOME="/Users/dallashall/Library/Application Support/PrismLauncher/java/java-runtime-epsilon" ./gradlew build
```

The output jar will be in `build/libs/SimpleAuth-1.0-SNAPSHOT.jar`

## Production Deployment

Deploy a full server with Java + Bedrock support:

```bash
# Build and deploy (one command)
./deploy-prod.sh

# Or manually:
./gradlew build
docker-compose -f docker-compose.prod.yml up -d
```

The production server includes:
- **Fabric 26.1.1** on Java 25
- **SimpleAuth** for authentication
- Java Edition: `localhost:25565`
- Op: `Dad`

> **Note:** Geyser/Floodgate (Bedrock support) is not yet available for Minecraft 26.1. Once they release 26.1 support, you can add them to the MODS list.

```bash
# View logs
docker-compose -f docker-compose.prod.yml logs -f mc

# Access console
docker exec -it mc-server rcon-cli

# Stop server (keep data)
docker-compose -f docker-compose.prod.yml down

# Stop and wipe data
docker-compose -f docker-compose.prod.yml down -v
```

## Testing with Docker

A lightweight test environment for development:

```bash
# Build and start test server (one command)
./test-server.sh

# Or manually:
./gradlew build
docker-compose up -d
docker-compose logs -f mc

# Stop server
docker-compose down

# Stop and wipe data
docker-compose down -v
```

The test server is a minimal setup:
- **Fabric Server** for Minecraft 26.1 on Java 25
- **SimpleAuth** (auto-mounted from `build/libs/`)
- No whitelist or additional mods
- Op: `Dad`

## Installation

1. Build the mod or download the jar
2. Place in your Fabric server's `mods/` folder
3. Restart the server
4. Database will be created at `config/simpleauth/auth.db`

## Usage

### Setting a player's password (console-only):

```
setpassword PlayerName their_secure_password
```

### Player login:

```
/login their_secure_password
```

## How it works

1. **Player joins** - They are locked (invulnerable, frozen, can't interact)
2. **Console sets password** - Admin runs `/setpassword PlayerName password` from console
3. **Player authenticates** - Player runs `/login password` in chat
4. **Lock released** - Player can now move and play normally

## Technical Details

- **Fabric for Minecraft 26.1** - First unobfuscated Minecraft version!
- **Mojang Mappings** - No Yarn, direct Mojang class names
- **Java 25** features (modern LTS)
- **BCrypt** with salt rounds = 12
- **Fabric API** for events and commands
- **SQLite** for persistent storage
- **No remapping** - Fast mod loading

## Security Notes

- Passwords are hashed with BCrypt before storage
- Only console can set passwords (prevents player abuse)
- Unauthenticated players are completely locked out
- Chat is blocked to prevent password leaking

## Future Enhancements

- [ ] Geyser form integration for Bedrock players
- [ ] Registration system (players can set their own password on first join)
- [ ] Session timeout (require re-auth after X minutes)
- [ ] Admin commands to view/manage accounts

## License

MIT

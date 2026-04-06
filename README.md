# SimpleAuth

Dead-simple authentication mod for Fabric on Minecraft 26.1 ("Tiny Takeover").

Secure your offline-mode server with password authentication, IP-based session caching, and spectator mode enforcement.

---

## Features

- **Console-only password management** - Admins control who can login
- **Spectator mode enforcement** - Unauthenticated players can't build, break, or interact
- **Remember me system** - Auto-login for players from known IPs
- **Configurable timeouts** - Auto-kick players who don't authenticate fast enough
- **Brute-force protection** - Locks out repeated failed login attempts
- **Secure password storage** - Industry-standard encryption

---

## Instructions for End Users

### Requirements

- Minecraft 26.1.1 (Java Edition)
- Fabric Loader 0.18.6+
- Fabric API 0.145.3+26.1.1
- Java 25+

### Installation Method 1: Manual Install

**1. Download the latest release:**
```bash
wget https://github.com/status-machina/SimpleAuth/releases/latest/download/SimpleAuth-1.2.0.jar
```

**2. Install on your server:**
- Place `SimpleAuth-1.2.0.jar` in your Fabric server's `mods/` folder
- Restart the server
- Database will be created at `config/simpleauth/auth.db`

**3. Set up passwords:**
- Access your server console
- Run `setpassword <username> <password>` for each player

### Installation Method 2: Docker Compose

**1. Copy the template:**
```bash
curl -O https://raw.githubusercontent.com/status-machina/SimpleAuth/main/docker-compose.prod.yml
```

**2. Customize for your server:**

Edit `docker-compose.prod.yml` and update:
- `OPS: Dad` → Change to your admin username
- `MEMORY: 6G` → Adjust memory allocation
- Ports if needed

**3. Deploy:**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

**4. Access console:**
```bash
docker exec -it mc-server rcon-cli
```

**5. Set up passwords:**
```bash
setpassword YourUsername password123
```

**Managing your server:**

View logs:
```bash
docker-compose -f docker-compose.prod.yml logs -f mc
```

Update to latest version:
```bash
docker-compose -f docker-compose.prod.yml restart
```
Old versions are automatically removed and latest is downloaded.

Stop server:
```bash
docker-compose -f docker-compose.prod.yml down
```

### Console Commands (Admins Only)

All commands must be run from the server console:

#### Set Player Password
```bash
setpassword <username> <password>
```
**Examples:**
```bash
setpassword Notch supersecret123
setpassword Dad mypassword
```

#### Configure Remember Me
```bash
setrememberusers <duration>
```
**Format:** Number followed by `d` for days

**Examples:**
```bash
setrememberusers 7d      # Remember for 7 days
setrememberusers 30d     # Remember for 30 days
setrememberusers 0d      # Disable remember me
```

#### Configure Auth Timeout
```bash
setauthtimeout <seconds>
```
**Format:** Number only (in seconds, no suffix)

**Examples:**
```bash
setauthtimeout 45        # 45 seconds (default)
setauthtimeout 120       # 2 minutes
setauthtimeout 30        # 30 seconds
```

**Valid range:** 10-600 seconds

> **Note:** Command formats differ - `setrememberusers` requires `d` suffix (e.g., `7d`), while `setauthtimeout` takes plain numbers (e.g., `45`).

### Player Commands

#### Login
```
/login <password>
```
**Example:**
```
/login supersecret123
```

Must be used within the timeout period (default 45 seconds) after joining.

### How It Works

**When a player joins:**
1. Immediately placed in **Spectator mode** (can't build/break/interact)
2. Authentication timeout starts (45 seconds by default)
3. Two possible paths:
   - **Remembered IP:** Auto-authenticated instantly
   - **New/expired session:** Must login with password

**If player doesn't authenticate in time:**
- Kicked with message: "Authentication timeout!"
- Can rejoin and try again

**After successful authentication:**
- Restored to original game mode (Creative, Survival, etc.)
- Can play normally
- Session saved if remember me is enabled

**Remember me system:**
- Saves IP + username after successful login
- Auto-authenticates on future joins from same IP
- Expires after configured duration (e.g., 7 days)
- Can be disabled with `setrememberusers 0d`

### Security Notes

- ✅ **Passwords hashed with BCrypt** - Industry standard, salt rounds = 12
- ✅ **Console-only password setting** - Players can't set or change passwords
- ✅ **Spectator mode enforcement** - Prevents griefing while unauthenticated
- ✅ **Everyone must authenticate** - Even OPs/admins (prevents username spoofing)
- ✅ **Timeout enforcement** - Kicks idle unauthenticated players
- ✅ **IP-based sessions** - Optional convenience with configurable expiration
- ✅ **Rate limiting** - 3 failed login attempts triggers a 10-second cooldown
- ✅ **Input validation** - Passwords must be 6-128 printable ASCII characters; usernames 3-16 alphanumeric
- ✅ **Session invalidation** - Password changes automatically revoke all remembered sessions
- ✅ **Audit logging** - All auth events logged (`AUTH_SUCCESS`, `AUTH_FAILURE`, `AUTH_TIMEOUT`, `PASSWORD_SET`, `CONFIG_CHANGE`)
- ✅ **Thread-safe** - Concurrent access protected with lock-free data structures
- ✅ **Clean shutdown** - Database and background tasks properly closed on server stop

**Why OPs must authenticate:**
Since this is designed for offline-mode servers, anyone can join with any username (including "Dad" or other admin names). Password authentication is the only security barrier.

### Troubleshooting

**Players can't login:**
- Verify password was set via console: `setpassword <username> <password>`
- Check server logs for `AUTH_FAILURE` entries
- Ensure player is using correct username (case-sensitive)
- Password must be 6-128 characters, printable ASCII only

**"Too many failed attempts" message:**
- Player hit the rate limit (3 failed attempts)
- Wait 10 seconds and try again
- Counter resets after 1 minute of no attempts

**Player auto-login stopped working after password change:**
- This is expected - password changes invalidate all remembered sessions
- Player must login with the new password; a new session will be saved

**Old version still loading after update:**
- Docker: Ensure `REMOVE_OLD_MODS: "TRUE"` is set in environment variables
- Manual: Remove old `SimpleAuth-*.jar` files from `mods/` folder and restart

**Database errors:**
- Ensure `config/simpleauth/` directory is writable
- Check disk space
- Verify no other process is locking `auth.db`

---

## Technical Details

### Architecture

**Password Storage:** BCrypt hashing with salt rounds = 12

**Database:** SQLite with three tables:
- `players` - Usernames, BCrypt password hashes, last login timestamps
- `remember_sessions` - IP + username pairs with expiration timestamps
- `config` - Server settings (remember duration, auth timeout)

**Database location:** `config/simpleauth/auth.db`

**Dependencies:**
- BCrypt (org.mindrot:jbcrypt:0.4) - Password hashing
- SQLite JDBC (org.xerial:sqlite-jdbc:3.45.1.0) - Database
- Fabric API 0.145.3+26.1.1 - Events and commands
- Fabric Loader 0.18.6+ - Mod loading

### Implementation Details

- **Unobfuscated Minecraft:** Uses Mojang mappings directly (26.1 is first unobfuscated version)
- **No remapping needed:** Faster mod loading
- **Java 25 features:** Modern LTS
- **Scheduled executor:** Handles authentication timeouts
- **Event-driven:** Uses Fabric API events for player join/disconnect
- **Game mode preservation:** Saves and restores player's original game mode

### Authentication Flow

```
Player Join
    ↓
Check session (IP + username)
    ↓
Valid session → Auto-authenticate → Restore game mode
    ↓
No session → Spectator mode → Start timeout → Wait for /login
    ↓
/login attempt → Rate limit check → Validate input → Verify password
    ↓
Success → Restore game mode → Save session (if remember enabled)
    ↓
Failure → Record attempt (3 failures = 10s cooldown)
    ↓
Timeout → Kick player
```

### Compatibility

- **Minecraft:** 26.1.1 (Java Edition)
- **Fabric Loader:** 0.18.6+
- **Fabric API:** 0.145.3+26.1.1
- **Java:** 25+

**Bedrock Edition:** Not currently supported (Geyser/Floodgate not yet compatible with 26.1)

---

## Local Development

### Building from Source

**Prerequisites:**
- Java 25 installed
- Git

**Build:**
```bash
git clone https://github.com/status-machina/SimpleAuth.git
cd SimpleAuth

# Set JAVA_HOME to Java 25 installation
export JAVA_HOME="/path/to/java25"

# Build
./gradlew build
```

**Output:** `build/libs/SimpleAuth-1.2.0.jar`

### Local Testing

**Start test server:**
```bash
./test-server.sh
```

**Or manually:**
```bash
./gradlew build
docker-compose up -d
docker-compose logs -f mc
```

**Test server includes:**
- Fabric 26.1.1 on Java 25
- SimpleAuth (auto-mounted from `build/libs/`)
- Creative mode, no whitelist
- Op: `Dad`

**Access console:**
```bash
docker exec -it mc-simpleauth-fabric rcon-cli
```

**Stop test server:**
```bash
docker-compose down
```

**Wipe data:**
```bash
docker-compose down -v
```

### Project Structure

```
SimpleAuth/
├── src/main/java/com/statusmachina/simpleauth/
│   ├── SimpleAuth.java          # Main mod class, auth state management
│   ├── AuthListener.java        # Player join/disconnect events
│   ├── AuthCommand.java         # /login command
│   ├── ConsoleCommand.java      # Console commands (setpassword, etc.)
│   ├── Database.java            # SQLite database
│   ├── RateLimiter.java         # Brute-force protection
│   └── InputValidator.java      # Username/password validation
├── src/main/resources/
│   └── fabric.mod.json          # Mod metadata
├── build.gradle.kts             # Build configuration
├── gradle.properties            # Version info
├── docker-compose.yml           # Test environment
├── docker-compose.prod.yml      # Production deployment
└── test-server.sh               # Quick test script
```

### Making Changes

1. Make your changes to the source code
2. Test locally: `./test-server.sh`
3. Commit changes: `git commit -am "Description"`
4. Create PR on GitHub

### Release Process

Releases are automated via GitHub Actions:

1. Update version in `gradle.properties`
2. Update version in `docker-compose.prod.yml`
3. Commit and push
4. Create tag: `git tag v1.x.x && git push origin v1.x.x`
5. GitHub Actions builds and creates release automatically

---

## Contributing

Contributions welcome! Please open an issue or pull request.

## License

MIT

## Links

- **GitHub:** https://github.com/status-machina/SimpleAuth
- **Releases:** https://github.com/status-machina/SimpleAuth/releases
- **Issues:** https://github.com/status-machina/SimpleAuth/issues

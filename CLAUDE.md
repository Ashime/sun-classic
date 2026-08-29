# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SUN Classic is a server-side reimplementation for the "Soul of the Ultimate Nation" (SUN) Classic game client. It is a multi-module Maven/Spring Boot project; each module is a separate network server that speaks a custom binary TCP protocol to either the game client or to other servers in this stack.

The client in use reports `VERSION = 2.6.0.1` (from `CLIENT_INFO.INI` inside the client's `System.wpk`), which is what `AuthServer.ini`'s `CLIENT_VERSION` is set to. Note the launcher capture in `Protocol.java` shows a `2.4.x` client version instead — the two have not been reconciled, so prefer the ini values and treat that capture as older.

**When working on anything that talks to the real game client, read `CLIENT-PROTOCOL-NOTES.md` first.** It is the living record of what has actually been observed on the wire (confirmed packet layouts, the little-endian framing fix, the open `A2U_ansVerify` blocker, `System.wpk`/`LOGIN.INI` editing, and environment setup outside the repo).

## Build & Run

This is a standard multi-module Maven reactor build (root `pom.xml`, packaging `pom`). There is no separate lint or test tooling configured beyond default Maven/Spring Boot behavior.

```
mvnw clean install -DskipTests                  # build + install all modules
mvnw -pl auth-server -am install -DskipTests    # build one module and the deps it needs
mvnw test                        # run tests (currently only a placeholder Spring Boot context test under root src/test)
```

Module build order matters: `commons` must be built/installed before dependent modules (`auth-server`, `database-server`, `game-server`, `web-server`, `launcher`) since they depend on its artifact. `-am` handles that within the reactor; a bare `-pl <module>` needs `commons` already installed to `~/.m2`.

**Run the servers from the packaged jars, started in the repo root:**

```
java -jar database-server/target/database-server-0.0.1-SNAPSHOT.jar
java -jar auth-server/target/auth-server-0.0.1-SNAPSHOT.jar
```

Do **not** use `mvnw -pl <module> spring-boot:run` — it sets the working directory to the *module* folder, while every `*Config` class resolves its ini relative to the repo root, so it fails immediately with `FileNotFoundException: ...\database-server\Config\DatabaseServer\DatabaseServer.ini`. (An IDE run configuration works if its working directory is set to the repo root.)

Start **database-server before auth-server**: `NioServer.initS2S()` connects out once with no retry, so an auth-server started first can never authenticate until restarted. On Windows a running server holds a lock on its own jar — stop it before rebuilding, or the `repackage` step fails with `Unable to rename ...jar`.

Servers are configured via `.ini` files (parsed with `ini4j`) under `Config/<ServerName>/`, not `application.properties`/YAML — e.g. `Config/AuthServer/AuthServer.ini`, `Config/DatabaseServer/DatabaseServer.ini`, `Config/DatabaseServer/DbManager/Tasks/*.ini`, `Config/GameServer/GameServer.ini`. Each server's `*Config` class (e.g. `AuthServerConfig`) is a singleton that loads its ini file into static fields once at startup — read the relevant `.ini` alongside the `*Config` class when tracing configuration-driven behavior. Requires JDK 25 and a running SQL Server 2025 instance (see `README.md` for full local setup instructions, including database restore and SSMS configuration).

The root-level `src/` directory (`com.valiantgaming.sunclassic.SunClassicApplication`) is leftover Spring Initializr scaffolding, not part of the reactor's module list, and is not meaningful application code.

## Architecture

### Modules
- **commons** — shared code used by every other module: the binary packet protocol contracts (`Protocol`, `Category`), session abstractions (`ClientSession`, `ServerSession`, `SessionManager`), crypto primitives (`AES`, `RSA`, `TEA`, `ARGON2`, `SHA`), and `IpFilter`/`Utility` helpers. Build this first when it changes.
- **database-server** — the authority for all persistent state (accounts, characters, inventory, equipment, server registry) via Hibernate (`HibernateSession`, `*DAO` classes, `database/entity/**`). Every other server connects to it as an S2S client and never touches SQL Server directly.
- **auth-server** — handles login/authentication and server-list selection for the game client (`network/packet/client/**`), and is itself an S2S client of database-server.
- **game-server**, **web-server**, **launcher** — scaffolded modules (mostly a bare `*Application` class today); expect to build out packet handling for these following the same patterns as `auth-server`.

### Networking model
Every server is built on Netty (`NioServer` in each module) and keeps **two separate pipelines**:
- **S2S (server-to-server)**: this server acting as a *client* connecting outward to database-server (or, from database-server's perspective, as the listener other servers connect to). Wired up in `NioServer.initS2S()`.
- **C2S (client-to-server)**: the game client connecting inward (`NioServer.initC2S()`). In `auth-server` this is **live**, and starts on its own thread (`c2s-listener`) because `initS2S()` blocks for as long as the database-server connection stays open — so the client listener comes up whether or not database-server is reachable.

A channel pipeline is generally: `ipFilter`/`uniqueIpFilter` → `idleStateHandler` (config-driven idle disconnect) → `frameDecoder` (`PacketFraming`, reassembles the 2-byte length-prefixed frame; **little-endian** — see `CLIENT-PROTOCOL-NOTES.md` §1) → `byteDecoder`/`byteEncoder` (raw `ByteArrayDecoder`/`Encoder`) → a module-specific `PacketDecoder`/`PacketEncoder` (validates the custom packet format) → a module-specific `PacketHandler`/`ServerPacketHandler` (dispatches on the packet's protocol byte).

### Packet format & dispatch
Packets are raw byte arrays: byte 0 is the `Category` (which server family, e.g. `Category.AUTH = 0x33`, `Category.DATABASE = 0x31`), byte 1 is the `Protocol` opcode (see `commons/.../packet/Protocol.java`, which documents known client/launcher packet layouts in comments). Handlers (`ChannelDuplexHandler` subclasses) switch on `message[1]` and dispatch to a request class (e.g. `AskAesFileKey`, builds/sends a packet) paired with a handler class (e.g. `GetAesFileKey`, validates/parses a response) — request and validation logic are deliberately kept in separate classes under `network/packet/server/` and `network/packet/server/handler/` (or the client-facing equivalents).

### S2S handshake (auth-server ↔ database-server today; the pattern any new S2S client should follow)
1. **Mutual TLS.** An `SslHandler` sits ahead of `ServerPacketHandler`, which waits on its `handshakeFuture()` before sending anything application-level. This **replaced** the old `S2S_askAesFileKey`/`S2S_askRsaKey`/`S2S_askAesKey` packet exchange — TLS now proves and encrypts the connection. Material comes from each server's `[TLS]` ini section (`CERT_PATH`/`KEY_PATH` identify this server, `CA_PATH` verifies the peer); the connection only completes if both sides present certificates signed by the trusted CA.
2. `S2S_askServerInfo` / `S2S_ansServerInfo` — sent immediately on TLS success; the server identifies itself (e.g. "AUTH SERVER") so database-server can associate the connection with a `ServerInfo` row.

Only after this succeeds should application-level packets (e.g. auth-server's `AnsAuthUser`) be trusted/sent.

The retired AES/RSA opcodes and their request/handler classes may still exist in `Protocol.java` and `network/packet/server/**`; treat them as dead unless you find a live caller. A healthy auth-server startup logs exactly: `Server successfully connected to 127.0.0.1:10000` → `TLS handshake completed with ...` → `Received ServerInfo response from ...`.

### Database-server internals
`DatabaseManager` (singleton, `@Service`) runs one-time startup tasks (loading `IpRules`/`ServerInfo` via a `taskExecutor`) and recurring scheduled tasks (e.g. deactivated-account deletion) via a `taskScheduler`, both sized from `Config/DatabaseServer/DbManager/DatabaseManager.ini`. Per-entity task timing (e.g. character/account deactivation-to-deletion windows) lives in `Config/DatabaseServer/DbManager/Tasks/*.ini`, read by matching `*Config` classes (`AccountConfig`, `CharacterConfig`). Several deletion code paths are stubbed pending stored procedures — check for `// TODO` markers in `DatabaseManager` before assuming a scheduled task is fully implemented.

Encryption keys themselves (AES key/IV, HMAC key) are stored via `EncryptionKeyDAO`/`EncryptionKey` and can be rotated at startup by toggling `RECREATE_AES_KEY` / `RECREATE_HMAC_KEY` in `DatabaseServer.ini` — note `RECREATE_HMAC_KEY = TRUE` forces a password rehash for every account (`AccountDAO.updateAllPasswords()`), so treat that flag as destructive on an existing database.

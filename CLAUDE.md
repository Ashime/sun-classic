# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SUN Classic is a server-side reimplementation for the "Soul of the Ultimate Nation" (SUN) Classic game client (client v2.4.0.2). It is a multi-module Maven/Spring Boot project; each module is a separate network server that speaks a custom binary TCP protocol to either the game client or to other servers in this stack.

## Build & Run

This is a standard multi-module Maven reactor build (root `pom.xml`, packaging `pom`). There is no separate lint or test tooling configured beyond default Maven/Spring Boot behavior.

```
mvnw clean install              # build all modules
mvnw -pl auth-server clean install       # build a single module (must install commons first if changed)
mvnw -pl auth-server spring-boot:run     # run the auth server
mvnw -pl database-server spring-boot:run # run the database server
mvnw test                        # run tests (currently only a placeholder Spring Boot context test under root src/test)
```

Module build order matters: `commons` must be built/installed before dependent modules (`auth-server`, `database-server`, `game-server`, `web-server`, `launcher`) since they depend on its artifact.

Servers are configured via `.ini` files (parsed with `ini4j`) under `Config/<ServerName>/`, not `application.properties`/YAML — e.g. `Config/AuthServer/AuthServer.ini`, `Config/DatabaseServer/DatabaseServer.ini`, `Config/DatabaseServer/DbManager/Tasks/*.ini`, `Config/GameServer/GameServer.ini`. Each server's `*Config` class (e.g. `AuthServerConfig`) is a singleton that loads its ini file into static fields once at startup — read the relevant `.ini` alongside the `*Config` class when tracing configuration-driven behavior. Requires JDK 25 and a running SQL Server 2025 instance (see `README.md` for full local setup instructions, including database restore and SSMS configuration).

The root-level `src/` directory (`com.valiantgaming.sunclassic.SunClassicApplication`) is leftover Spring Initializr scaffolding, not part of the reactor's module list, and is not meaningful application code.

## Architecture

### Modules
- **commons** — shared code used by every other module: the binary packet protocol contracts (`Protocol`, `Category`), session abstractions (`ClientSession`, `ServerSession`, `SessionManager`), crypto primitives (`AES`, `RSA`, `TEA`, `BCRYPT`, `SHA`), and `IpFilter`/`Utility` helpers. Build this first when it changes.
- **database-server** — the authority for all persistent state (accounts, characters, inventory, equipment, server registry) via Hibernate (`HibernateSession`, `*DAO` classes, `database/entity/**`). Every other server connects to it as an S2S client and never touches SQL Server directly.
- **auth-server** — handles login/authentication and server-list selection for the game client (`network/packet/client/**`), and is itself an S2S client of database-server.
- **game-server**, **web-server**, **launcher** — scaffolded modules (mostly a bare `*Application` class today); expect to build out packet handling for these following the same patterns as `auth-server`.

### Networking model
Every server is built on Netty (`NioServer` in each module) and keeps **two separate pipelines**:
- **S2S (server-to-server)**: this server acting as a *client* connecting outward to database-server (or, from database-server's perspective, as the listener other servers connect to). Wired up in `NioServer.initS2S()`.
- **C2S (client-to-server)**: the game client connecting inward (`NioServer.initC2S()`) — currently disabled/commented out in `auth-server`'s `NioServer` pending the S2S handshake being finished first.

A channel pipeline is generally: `ipFilter`/`uniqueIpFilter` → `idleStateHandler` (config-driven idle disconnect) → `byteDecoder`/`byteEncoder` (raw `ByteArrayDecoder`/`Encoder`) → a module-specific `PacketDecoder`/`PacketEncoder` (frames/validates the custom packet format) → a module-specific `PacketHandler`/`ServerPacketHandler` (dispatches on the packet's protocol byte).

### Packet format & dispatch
Packets are raw byte arrays: byte 0 is the `Category` (which server family, e.g. `Category.AUTH = 0x33`, `Category.DATABASE = 0x31`), byte 1 is the `Protocol` opcode (see `commons/.../packet/Protocol.java`, which documents known client/launcher packet layouts in comments). Handlers (`ChannelDuplexHandler` subclasses) switch on `message[1]` and dispatch to a request class (e.g. `AskAesFileKey`, builds/sends a packet) paired with a handler class (e.g. `GetAesFileKey`, validates/parses a response) — request and validation logic are deliberately kept in separate classes under `network/packet/server/` and `network/packet/server/handler/` (or the client-facing equivalents).

### S2S handshake (auth-server ↔ database-server today; the pattern any new S2S client should follow)
1. `S2S_askAesFileKey` / `S2S_ansAesFileKey` — client asks database-server to decrypt `Config/key.enc` using an AES file key computed from the channel ID; response is validated via HMAC (`GetAesFileKey`/`SHA`).
2. `S2S_askRsaKey` / `S2S_ansRsaKey` — RSA public key exchange, MAC-verified.
3. `S2S_askAesKey` / `S2S_ansAesKey` — session AES key exchange; once validated, packet-level encryption (`messageCryptEnabled`) is turned on for the session (tracked per-connection in a `ServerSession`, held by that module's `ServerSessionManager`).
4. `S2S_askServerInfo` / `S2S_ansServerInfo` — server identifies itself (e.g. "AUTH SERVER") so database-server can associate the connection with a `ServerInfo` row.

Only after this handshake succeeds should application-level packets (e.g. auth-server's `AnsAuthUser`) be trusted/sent.

### Database-server internals
`DatabaseManager` (singleton, `@Service`) runs one-time startup tasks (loading `IpRules`/`ServerInfo` via a `taskExecutor`) and recurring scheduled tasks (e.g. deactivated-account deletion) via a `taskScheduler`, both sized from `Config/DatabaseServer/DbManager/DatabaseManager.ini`. Per-entity task timing (e.g. character/account deactivation-to-deletion windows) lives in `Config/DatabaseServer/DbManager/Tasks/*.ini`, read by matching `*Config` classes (`AccountConfig`, `CharacterConfig`). Several deletion code paths are stubbed pending stored procedures — check for `// TODO` markers in `DatabaseManager` before assuming a scheduled task is fully implemented.

Encryption keys themselves (AES key/IV, HMAC key) are stored via `EncryptionKeyDAO`/`EncryptionKey` and can be rotated at startup by toggling `RECREATE_AES_KEY` / `RECREATE_HMAC_KEY` in `DatabaseServer.ini` — note `RECREATE_HMAC_KEY = TRUE` forces a password rehash for every account (`AccountDAO.updateAllPasswords()`), so treat that flag as destructive on an existing database.

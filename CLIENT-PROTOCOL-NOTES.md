# Game Client ↔ AuthServer — findings and outstanding work

Working notes from connecting the **real** SUN Classic client (`Sungame.exe`, client
`VERSION = 2.6.0.1`) to this stack. Everything below was observed against a live client, not
inferred — where something is still a guess it says so.

Status date: 2026-08-27 (updated late session — §6 resolved, §3 substantially revised).

---

## 1. Solved: packet length header is little-endian

**This is fixed in code.** Every `PacketEncoder`/`PacketDecoder` used to call
`Utility.flip(header, 0, 1)`, which byte-swapped the 2-byte length into big-endian. Our own
servers agreed with each other, so launcher ↔ auth-server worked and the bug stayed hidden —
but the real client reads `00 06` as `0x0600` (1536) and waits for bytes that never arrive,
then times out.

Confirmed twice:
- the capture already in `Protocol.java`: `Packet Size: 0e 00` for a 14-byte message;
- a live client replies to `A2U_ansReady` only when its header is little-endian.

Removed the swap at all 10 sites (5 encoders + 5 decoders across auth-server, database-server,
web-server, launcher) and set `PacketFraming`'s Netty frame decoder to
`ByteOrder.LITTLE_ENDIAN` — it is shared by every pipeline, so it had to move in lockstep.

**Do not reintroduce a `flip` on the length header.**

---

## 2. Confirmed handshake — `U2A_askVerify` layout is now exact

Server speaks first. A silent listener proved the client sends nothing on connect and waits.

```
S->C  06 00 | 33 00 | <4-byte TEA key>            A2U_ansReady      (ACCEPTED by client)
C->S  25 00 | 33 01 | 07 01 01 | <32-byte host>   U2A_askVerify     (37 bytes on the wire)
S->C  ?                                           A2U_ansVerify     (format still unknown, §3)
```

`U2A_askVerify` payload (35 bytes after category+protocol), verified byte-for-byte on a live
client 2026-08-27:

```
07 01 01                                  3-byte prefix, meaning unknown
63 6F 6E 6E 65 63 74 65 64 31 2E 73 75 6E 63   "connected1.sunc"  (15 chars)
00 × 17                                   null padding to a 32-byte field
```

So the host field is **32 bytes, null-padded, and the hostname is truncated to 15 characters** —
`connected1.sunclassic.webzen.co.kr` arrives as `connected1.sunc`. This is now parsed properly
by `VerifyUser` (see §3.1).

Our `A2U_ansReady` **content was correct all along** — only its header framing was wrong.

---

## 3. OPEN: `A2U_ansVerify` format is still unknown

This remains the blocker. Nothing past step 2 has ever been exercised.

### 3.1 Fixed: we were actively rejecting the client

`VerifyUser.verify()` was a port of the old LoginServer's `VerifyIpProtocol.verify()`, written
before this packet had ever been captured. It tested the payload for `AuthServerConfig`'s client
protocol (`346`) and client IP (`127001`) — **neither of which appears anywhere in this packet**,
which carries a hostname. It therefore always returned false, and `AnsVerify` sent result byte
`0x01`, a hard refusal.

Rewritten to parse the real §2 layout: validate the 35-byte structure, extract and log the login
host, and fail only on a genuinely malformed request. It deliberately does **not** compare the
host against config — that value lives in `LOGIN.INI` inside the client's `System.wpk`, which the
server cannot see. Gating a hard refusal on a check we cannot actually perform is what caused
this.

Log line to look for: `VerifyUser - Client login host: 'connected1.sunc'`.

### 3.2 The timing table was over-read — do not trust it

Earlier conclusions ranked candidates by how long the client waited before closing. **That signal
is not reproducible.** The same three bytes (`33 02 00`) produced three very different closes:

| when | response | close after |
|---|---|---|
| pre-§1 framing fix | `33 02 00` | 3.8s |
| 2026-08-27 22:55 | `33 02 00` | 14.8s |
| 2026-08-27 23:02 | `33 02 00` | 5.2s |

Other single data points, kept for the record but subject to the same caveat: no reply → 13.3s;
37-byte mirror → 1.9s; `33 02 01` (hard reject) → 19.5s.

Close-delay is therefore **not** a clean function of what we send. It plausibly also depends on
which retry attempt it is and on client-side state. Earlier readings like "3.8s means waiting for
more bytes" should be treated as unsupported.

**Rank candidates on qualitative signals instead:** does the client send *any* inbound byte after
our reply, or advance to `U2A_askAuthUser`? To date, no candidate has produced either.

### 3.3 `SERVICE_LOGIN_TRY_COUNTS = 10` does NOT give ten attempts

Previously recorded as a "useful lever" for testing many candidates per launch. **This is wrong.**
Observed identically across three launches (22:49, 22:54, 23:02):

```
connect #1 → closes immediately, sends nothing
connect #2 → sends U2A_askVerify, receives our reply, closes
             ...client process exits. No further attempts.
```

Exactly **one `askVerify` exchange per launch.** The retry budget may only apply to failures to
*connect* (connection refused), not to a connection that succeeds and gets an unsatisfactory
reply — untested hypothesis.

**Consequence: each candidate costs one full client launch (~45s).** The per-connection probe in
§8 still works mechanically, but it advances one candidate per launch, not ten.

### 3.4 Candidates

Tested, no inbound byte and no progression:
- `33 02 00` (3-byte) — the current non-probe default
- `33 02 01` — hard reject
- `33 02 07 01 01` + 32-byte host echo (mirror)

Untested, already wired into the probe rotation (§8):
`empty body`, `0000`, `00000000`, `0000000000000000`, `01`, `070101`, `00`+host, `0001`+host.

---

## 4. `System.wpk` is editable — this is where the server address lives

`System\LOGIN.INI` (323 bytes at `0x0C2B1018` inside `<client>\System\System.wpk`) is
**plaintext**, though the archive's header and its other payloads are obfuscated — a byte-scan of
all 195 MB for `LOGIN_SERVER_TYPE` / `LOGIN.INI` finds nothing, so `wpktool` is the only way in.

Original (and current) contents:

```ini
LOGIN_SERVER_TYPE = 2
SERVICE_LOGIN_SERVER_NUM = 1
SERVICE_LOGIN_SERVER_PORT = 44405
SERVICE_LOGIN_SERVER_IP_HEAD = connected
SERVICE_LOGIN_SERVER_IP_TAIL = sunclassic.webzen.co.kr
SERVICE_LOGIN_TRY_COUNTS = 10
```

The client composes the address as `HEAD + <index> + "." + TAIL`, producing
`connected1.sunclassic.webzen.co.kr:44405` — confirmed by §2, where the client reports exactly
that host (truncated) back to us.

Tooling: `<client>\System\wpktool.exe` (**requires elevation**).
- `-l <wpk>` list, `-e <wpk> <addr> <size> <dest>` extract, `-m <wpk> <name> <file>` merge back.
- `-m` patches the slot **in place**, so the replacement must be **exactly 323 bytes**. Pad
  shorter content with a trailing `;` comment line — INI-inert, and the file already uses a
  `;;;;;` divider so it matches the existing style.
- Verify a merge by extracting the entry straight back out and comparing hashes. Always back up
  `System.wpk` first (195 MB); a verified `System.wpk.bak` makes every experiment cheap to undo.
- The Korean comments are **CP949**, not UTF-8. Edit at byte level (or via Latin-1 round-trip,
  which is byte-exact) or they will be corrupted.

### Attempted and reverted (all restored; archive is byte-identical to the original)

| attempt | result |
|---|---|
| `HEAD = 127.0.0.`, empty `TAIL`, `TYPE = 2` | merged and read back fine; client never connected |
| `TYPE = 1`, `IP_HEAD = 127.0.0.1`, no `TAIL`, `NUM` removed | merged and verified; not tested against a client before revert |
| `TYPE = 1`, `IP_HEAD = 127.0.0.1`, `NUM = 1` restored | merged and verified; not tested against a client before revert |

**Unresolved:** whether the client can be given a bare IP at all. If `HEAD + index + "." + TAIL`
always inserts the index and dot, `IP_HEAD = 127.0.0.1` composes to `127.0.0.11.` — invalid.
Whether `LOGIN_SERVER_TYPE = 1` selects a different composition rule was never actually put in
front of a client. The hosts-entry route (§5) works and is the known-good configuration, so this
is a cleanup nicety, not a blocker.

Also inside: `System\CLIENT_INFO.INI` → `VERSION = 2.6.0.1`, and `System\PROGRAM.INI`.

---

## 5. Environment changes made (outside the repo)

These live on the machine, not in git.

| change | status | purpose / undo |
|---|---|---|
| **hosts entry** `127.0.0.1 connected1.sunclassic.webzen.co.kr` | **active, required** | routes the client's login host to local auth-server. Undo: delete the marked lines in `C:\Windows\System32\drivers\etc\hosts` (admin), then `ipconfig /flushdns` |
| **Firewall**: 2 rules blocking `Sungame.exe` / `SUN.exe` outbound to `125.141.214.0/24` | **REMOVED — do not re-add**, see §6 | was intended to stop the client reaching Webzen's live servers |
| **SQL Server grants** on `sun-classic` for login `SunClassic` | active, required | `AuthenticateAccount`, `GetAccountCredentials`, `GetDeactivatedAccounts` had no EXECUTE. Undo: `REVOKE EXECUTE ON OBJECT::dbo.<proc> FROM SunClassic` |

The client requires its Webzen HTTP version check (AWS Seoul IPs, contacted by IP — no hostname
in DNS cache, so hosts cannot intercept it) to succeed or it will not boot. Blocking
`Sungame.exe` outbound wholesale prevents the client from starting at all.

---

## 6. RESOLVED — the firewall rules were the regression

Previously: "the client no longer reaches `127.0.0.1:44405` at all."

**Cause confirmed: the two outbound firewall rules in §5.** Removing them restored connectivity
immediately — the client connected on the very next launch and completed the §2 handshake, with
no other change in play (`LOGIN.INI` had already been restored to the original, hosts entry
unchanged, servers freshly started).

The `125.141.214.0/24` block was too broad: the client evidently needs to reach something in that
range before it will use the login server at all. Containment now rests on the hosts entry alone,
which already prevents the live login server being used.

**Do not re-add those rules** without a much narrower target and a retest.

---

## 7. Other known gaps

- **`A2U_ansVerify` format** — §3, the live blocker.
- **No S2S reconnect.** `NioServer.initS2S()` connects to database-server once with no retry, so
  any drop leaves auth-server permanently unable to authenticate until restarted. Start
  database-server **before** auth-server. This masked a bug once already: auth-server
  short-circuited on `No active connection to Database Server` and returned an instant "not
  authenticated", which looked like a real credential rejection.
- **Client handoff token.** `LauncherController#onStartGame` passes the account password via
  `-User:` / `-Password:`. The real client expects a short-lived token from the Webzen Web
  Starter (`Protocol.java` header; the official launcher logs
  `ExecuteProgram ... 42126697|AioHaruka||<token>|2|1|1|2|2`). `ClientSession#password` is a
  stopgap. An attempt to also pass `-IP:` was made and reverted — it did not help, and the
  address comes from `LOGIN.INI` anyway (§4). Note the client's own `start-game.bat` uses
  `-Username:` where `Protocol.java`'s capture note says `-User:`; unresolved which is correct.
- **`mvnw -pl <module> spring-boot:run` does not work.** `spring-boot:run` sets the working
  directory to the *module* folder, but every `*Config` class resolves its ini relative to the
  repo root, so it dies with
  `FileNotFoundException: ...\database-server\Config\DatabaseServer\DatabaseServer.ini`.
  Run the fat jars from the repo root instead (works, and is what the commands in `CLAUDE.md`
  should be corrected to):
  ```
  java -jar database-server/target/database-server-0.0.1-SNAPSHOT.jar
  java -jar auth-server/target/auth-server-0.0.1-SNAPSHOT.jar
  ```
  Windows holds a lock on a running jar, so stop a server before rebuilding it.
- **`SUN7CL.ini`** (client root) is genuinely encrypted — 7.887 bits/byte entropy, no
  repeating-key or single-byte XOR structure. Not worth attacking; `LOGIN.INI` in `System.wpk`
  supersedes it.

---

## 8. Tooling

**In the repo now:**

- `AnsVerifyProbe` (`auth-server/.../network/packet/client/`) — serves a different
  `A2U_ansVerify` candidate per connection and logs the close-delay for each, so candidates can
  be walked without a rebuild between each one. Enabled by `[PROBE] ANS_VERIFY_PROBE` in
  `AuthServer.ini`; **leave `FALSE` for normal operation**, since it deliberately sends replies
  known to be wrong. Read results by pairing the `PROBE serving ...` and `PROBE result: ...`
  lines. Per §3.3 it advances one candidate per client launch, and per §3.2 judge the result on
  whether any inbound byte arrives, not on the timing. The cursor resets when auth-server
  restarts.

**Session scratchpad only (not in the repo, worth porting if this continues):**

- `RawProtocolProbe.java` — listens on 44405, optionally silent, sends arbitrary hex and logs
  both directions.
- `VerifyProbe.java` — standalone protocol-aware probe, superseded by `AnsVerifyProbe`.
- `LaunchGameProbe.java` / `ClientLaunchProbe.java` — drive `GameClientLauncher` headlessly.
- `phase-*.ps1` — elevated `wpktool` wrappers: verified backup, size-padding merge with
  round-trip hash verification, and verified restore.
- `wpk-extract/LOGIN.INI.orig` — pristine copy of the 323-byte original, MD5
  `6b70d1c9cd19e8edbfc9896d280da0c5`.

**Reference hashes** for the client install, so a modified file can always be spotted:

| artifact | hash |
|---|---|
| pristine `System.wpk` | SHA256 `7DE3B13423DB0EE72335F589713217C36F08F7137542FC5487C1A0D741A0A76C` |
| pristine `LOGIN.INI` (323 bytes) | MD5 `6b70d1c9cd19e8edbfc9896d280da0c5` |

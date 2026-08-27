# Game Client ↔ AuthServer — findings and outstanding work

Working notes from connecting the **real** SUN Classic client (`Sungame.exe`, client
`VERSION = 2.6.0.1`) to this stack for the first time. Everything below was observed against a
live client, not inferred — where something is still a guess it says so.

Status date: 2026-08-27.

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

## 2. Confirmed handshake so far

Server speaks first. A silent listener proved the client sends nothing on connect and waits.

```
S->C  06 00 | 33 00 | <4-byte TEA key>            A2U_ansReady      (ACCEPTED by client)
C->S  25 00 | 33 01 | 07 01 01 | <32-byte name>   U2A_askVerify     (37-byte body)
S->C  03 00 | 33 02 | 00                          A2U_ansVerify     (REJECTED - placeholder)
```

`U2A_askVerify` body layout (37 bytes): `07 01 01` then a 32-byte null-padded field containing
`connected1.sunc` — the login hostname truncated to 15 chars + 17 nulls. 2 + 3 + 32 = 37.

Our `A2U_ansReady` **content was correct all along** — only its header framing was wrong.

---

## 3. OPEN: `A2U_ansVerify` format is unknown

`AnsVerify.createPacket()` returns a placeholder `{ AUTH, A2U_ansVerify, 0x00 }`. The client
rejects it. This is the current blocker — nothing past step 2 has been exercised.

Evidence gathered, by client close-time after our reply:

| response sent | bytes | client closed after | reading |
|---|---|---|---|
| *(nothing)* | — | 13.3s | idle timeout |
| `33 02 00` | 3 | 3.8s | likely waiting for more — a read timeout |
| `33 02 07 01 01` + 32-byte name echo (mirror) | 37 | 1.9s | actively rejected |

So the real response is **probably longer than 3 bytes and is not a mirror of the request**.
Untested candidates already prepared: `33020000`, `330200000000`, `3302000000000000`,
`330201`, `3302070101`, `330200` + 32-byte name.

**Useful lever:** `SERVICE_LOGIN_TRY_COUNTS = 10` (see §4) means the client retries the login
server ten times per launch. `scratchpad/VerifyProbe.java` was updated to serve a *different*
candidate per connection, so one launch can test many candidates instead of one. That run was
blocked by §6 before producing results.

---

## 4. `System.wpk` is editable — this is where the server address lives

`System\LOGIN.INI` (323 bytes at `0x0C2B1018` inside
`<client>\System\System.wpk`) is **plaintext**:

```ini
LOGIN_SERVER_TYPE = 2
SERVICE_LOGIN_SERVER_NUM = 1
SERVICE_LOGIN_SERVER_PORT = 44405
SERVICE_LOGIN_SERVER_IP_HEAD = connected
SERVICE_LOGIN_SERVER_IP_TAIL = sunclassic.webzen.co.kr
SERVICE_LOGIN_TRY_COUNTS = 10
```

The client composes the address as `HEAD + <index> + "." + TAIL`, producing
`connected1.sunclassic.webzen.co.kr:44405` — exactly what it was observed resolving.

Tooling: `<client>\System\wpktool.exe` (needs elevation).
- `-l <wpk>` list, `-e <wpk> <addr> <size> <dest>` extract, `-m <wpk> <name> <file>` merge back.
- **`-m` only accepts a replacement of exactly the original byte size** (it patches the slot in
  place). Pad shorter content with a comment line; it rejects the merge otherwise, harmlessly.

Also inside: `System\CLIENT_INFO.INI` → `VERSION = 2.6.0.1` (matches what auth-server reported),
and `System\PROGRAM.INI`.

### Attempted and reverted
Set `HEAD = 127.0.0.` with an empty `TAIL`, hoping to get a literal `127.0.0.1` and drop the
hosts entry. Merge succeeded and read back correctly, but the client then never connected —
**and it still did not connect after restoring the original**, so this is not proof the format
is wrong (see §6). `LOGIN.INI` has been restored; MD5 verified identical to the original.

**Unresolved:** whether the client can be given a bare IP at all. If `HEAD + index + "." + TAIL`
always inserts the dot, an empty `TAIL` yields `127.0.0.1.` (trailing dot), which likely fails to
resolve. A safe alternative is to point HEAD/TAIL at a non-Webzen hostname we control and map
that in hosts — that removes the Webzen domain from the client but still needs a hosts entry.

---

## 5. Environment changes made (outside the repo)

These live on the machine, not in git. Undo instructions included.

| change | purpose | undo |
|---|---|---|
| **hosts entry** `127.0.0.1 connected1.sunclassic.webzen.co.kr` | routes the client's login host to the local auth-server | delete the 2 marked lines in `C:\Windows\System32\drivers\etc\hosts` (admin), then `ipconfig /flushdns` |
| **Firewall**: 2 rules blocking `Sungame.exe` / `SUN.exe` outbound to `125.141.214.0/24` | stop the client reaching Webzen's live login/game server | `Get-NetFirewallRule -DisplayName 'SUN Classic local dev*' \| Remove-NetFirewallRule` |
| **SQL Server grants** on `sun-classic` for login `SunClassic` | `AuthenticateAccount`, `GetAccountCredentials`, `GetDeactivatedAccounts` had no EXECUTE — login could never work | `REVOKE EXECUTE ON OBJECT::dbo.<proc> FROM SunClassic` |

The client requires its Webzen HTTP version check (AWS Seoul IPs, contacted by IP — no hostname
in DNS cache, so hosts cannot intercept it) to succeed or it will not boot. Blocking
`Sungame.exe` outbound wholesale prevents the client from starting at all.

---

## 6. REGRESSION — investigate first

**The client no longer reaches `127.0.0.1:44405` at all.** It boots, does its HTTP check, then
exits ~38s later without connecting.

It connected reliably *before* any firewall rule existed, and has not since — including with
only the narrow `125.141.214.0/24` scoped block, and with `LOGIN.INI` restored to original. That
makes the firewall rules the prime suspect: the client likely needs to reach something else in
that `/24` (a server-list or presence service) before it will use the login server.

Strongly correlated, not yet proven. **Next step: remove the two firewall rules and retest.** If
the client connects again, the `/24` block is too broad and containment needs a narrower target
(or must be dropped, since the hosts entry already prevents the live login server being used).

Note `SERVICE_LOGIN_TRY_COUNTS = 10` explains an earlier misreading: the client retrying and
exhausting attempts looks like a boot failure, but is just retry exhaustion.

---

## 7. Other known gaps

- **No S2S reconnect.** `NioServer.initS2S()` connects to database-server once with no retry, so
  any drop leaves auth-server permanently unable to authenticate until restarted. This masked a
  bug once already: auth-server short-circuited on `No active connection to Database Server` and
  returned an instant "not authenticated", which looked like a real credential rejection.
- **Client handoff token.** `LauncherController#onStartGame` passes the account password via
  `-User:` / `-Password:`. The real client expects a short-lived token from the Webzen Web
  Starter (`Protocol.java` header; the official launcher logs
  `ExecuteProgram ... 42126697|AioHaruka||<token>|2|1|1|2|2`). `ClientSession#password` is a
  stopgap and should be dropped once auth-server can issue a real token.
- **`SUN7CL.ini`** (client root) is genuinely encrypted — 7.887 bits/byte entropy, no repeating-key
  or single-byte XOR structure. Not worth attacking; `LOGIN.INI` in `System.wpk` supersedes it.

---

## 8. Reusable tooling

In this session's scratchpad (`.../scratchpad/`), not in the repo:

- `RawProtocolProbe.java` — listens on 44405, optionally stays silent to see whether the client
  speaks first, then sends arbitrary hex packets and logs both directions.
- `VerifyProbe.java` — protocol-aware: sends `A2U_ansReady`, waits for `U2A_askVerify`, then
  serves one `ansVerify` candidate **per connection** and flags a reply carrying `U2A_askAuthUser`.
- `LaunchGameProbe.java` / `ClientLaunchProbe.java` — drive `GameClientLauncher` headlessly.
- `wpk-*.ps1`, `login-ini-*.ps1` — elevated wrappers around `wpktool`, incl. the size-padding
  merge and the verified restore.
- `wpk-extract/LOGIN.INI.orig` — pristine copy, MD5 `6b70d1c9cd19e8edbfc9896d280da0c5`.

Worth moving into the repo if this work continues, since scratchpad is session-scoped.

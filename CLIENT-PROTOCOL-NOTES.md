# Game Client ↔ AuthServer — findings and outstanding work

Working notes from connecting the **real** SUN Classic client (`Sungame.exe`, client
`VERSION = 2.6.0.1`) to this stack. Everything below was observed against a live client, not
inferred — where something is still a guess it says so.

Status date: 2026-09-03 (§14: opcode sweep complete - no ansVerify reply exists in 0x00-0x1F;
`A2U_ansAuthUser = 0x0E` confirmed against the live client and drives it to `askSrvList`.
§16: the client reached Server Select and picked a server - AnsSrvList accepted by the live
client, all three of §15.2 s guessed bytes correct. Blocker is now A2U_ansSrvSelect (§16.3).
§17 records every change made; §18 hands off the server/channel data layer
(GameServerInfo and ChannelInfo tables already exist and are empty; ServerInfo is the wrong
source for the client-facing list).
§10-§18 added; §3.1 and §9.6 corrected by §11, §10.1 by §12.1, §3.3 obsoleted by §13.1,
§9.5 disproved by §14.1.)

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

> **Read §10.1 before trusting anything in this section.** Every candidate here was tested with
> `UniqueIpFilter` unconditionally on, which silently closes any second connection from
> `127.0.0.1` — and "no inbound byte, no progression" was the only signal used to rule candidates
> out. The controls are worth re-running with `UNIQUE_IP_FILTER = FALSE`.

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

**CORRECTED 2026-09-03 by §11 — the claim that neither value appears in the packet is wrong.**
The EP1 `ClientPackets` documentation names the two `askVerify` fields `ClientProtocol (3 bytes)`
and `ServerConnectionIP (32 bytes)`, which is exactly the §2 capture: `07 01 01` **is** the client
protocol, written one byte per component, and the 32-byte field **is** the address. Both checks are
therefore makeable after all — they were failing because `CLIENT_PROTOCOL` was set to `3.4.6`
instead of `7.1.1`, and because `LOGIN_SERVER_TYPE = 2` put a truncated hostname in the address
field instead of an address. With `CLIENT_PROTOCOL = 7.1.1` and `TYPE = 1` (§4) both now match, and
`VerifyUser` compares them again — parsing each field explicitly and logging the reason for every
rejection, since a silent `false` here is indistinguishable from the client ignoring our reply.

**Do not enable the address check under `LOGIN_SERVER_TYPE = 2`**: it sends `connected1.sunc`, which
cannot match `[CLIENT] IP`, and a mismatch is a hard refusal.

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

> **OBSOLETE — see §13.1.** The client will happily send a second `U2A_askVerify` on the same
> connection when it is re-sent `33 00 00`. Candidates no longer cost a launch each.

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
- `33 02 00` (3-byte) — the current non-probe default. Re-confirmed 2026-09-01 (probe candidate 1).
- `33 02 01` — hard reject
- `33 02 07 01 01` + 32-byte host echo (mirror). Re-confirmed 2026-09-01 (probe candidate 2).
- `33 02` — empty body. Tested 2026-09-01 (probe candidate 3).

Also tested 2026-09-01, all with the same signature (no inbound byte, no progression):
`0000` (cand 4), `00000000` (cand 5), `0000000000000000` (cand 6), `01` (cand 7).

Untested, remaining in the rotation (§8): `070101` (8), `00`+host (9), `0001`+host (10).

**The result-byte polarity question is now settled — it is not the issue.** The worry was that
`0x00` = success came from our own `AnsAuthUser` rather than from the client. Candidate 7 tested
`33 02 01` as a *success* reply and it failed exactly like `33 02 00`. Neither polarity is
accepted, so the result byte is not what the client is rejecting.

**Seven structurally different bodies (0, 1, 2, 4, 8 and 35 bytes, both polarities) have now all
failed identically.** Combined with §9.4 — where full Ghidra analysis found no opcode switch and
no literal `0x33`/`0x02` comparison anywhere — the body shape is very unlikely to be the variable
that matters. Continuing to sweep it is low-value; the dynamic approach in §9.4 is the way
forward.

---

## 4. `System.wpk` is editable — this is where the server address lives

`System\LOGIN.INI` is 323 bytes at `0x0C2B1018` inside `<client>\System\System.wpk` (confirmed by
`wpktool -l`). A byte-scan of all 195 MB for `LOGIN_SERVER_TYPE` / `LOGIN.INI` finds nothing.

**It is NOT plaintext in the archive — it is XOR `0x69`.** An earlier revision of this section said
plaintext; that was wrong, and it matters because the raw bytes at that offset MD5 to
`437cd1c38252473fe19f499c1cb41c1c`, not the `6b70d1c9…` recorded below. `wpktool` de-obfuscates on
extract and re-obfuscates on merge, so **`wpktool` works in decoded/plaintext space** and the
`6b70d1c9…` hash refers to the decoded form. (`0x0D^0x69='d'`, `0x0A^0x69='c'`, `0x20^0x69='I'`,
`0x3D^0x69='T'` — the giveaway is `dc` for CRLF and `ITI` for ` = ` in a raw dump.)

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
- **`-m` also requires the replacement's first 4 bytes to match the existing entry's.** Otherwise
  it refuses with a Shift-JIS error that renders as mojibake
  (`Éµô¬âfü[â^4âoâCâgé¬òsêΩÆv` = `先頭データ4バイトが不一致`, "the first 4 bytes of data do not
  match") and leaves the archive untouched. The original starts `; CR LF ;` (`3B 0D 0A 3B`), so
  **begin any replacement with a `;` comment line**.
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

### RESOLVED 2026-09-01 — a direct IP works, and this is now the active configuration

`LOGIN_SERVER_TYPE = 1` **does** select a different address scheme, and it works against a live
client. Decompiling `CLoginGameParam::Load` (`FUN_004f50b0`) gives the exact key→field mapping:

| ini key | default | field | used when |
|---|---|---|---|
| `LOGIN_SERVER_IP` | `10.1.28.143` | `+0x458` (string) | **`TYPE != 2`** |
| `LOGIN_SERVER_PORT` | `0x2774` (10100) | `+0x470` | **`TYPE != 2`** |
| `LOGIN_SERVER_TYPE` | `0` | `+0x474` | always |
| `SERVICE_LOGIN_SERVER_IP_HEAD` | `connect` | string | `TYPE == 2` |
| `SERVICE_LOGIN_SERVER_IP_TAIL` | `sunonline.co.kr` | string | `TYPE == 2` |
| `SERVICE_LOGIN_SERVER_PORT` | `0xad75` (44405) | `+0x4b0` | `TYPE == 2` |
| `SERVICE_LOGIN_SERVER_NUM` | `2` | `+0x4b4` | `TYPE == 2` |
| `SERVICE_LOGIN_TRY_COUNTS` | `10` | `+0x4b8` | — |

**Why §4's earlier attempts could never have worked:** they set `IP_HEAD = 127.0.0.1`, but
`TYPE = 1` ignores `IP_HEAD` entirely and reads `LOGIN_SERVER_IP` — which was absent from the
file, so the client fell back to the built-in default `10.1.28.143`. The right keys were simply
never present.

The merged 323-byte `LOGIN.INI` now in `System.wpk`:

```ini
;
;
[PARAM]
LOGIN_SERVER_TYPE = 1
LOGIN_SERVER_IP = 127.0.0.1
LOGIN_SERVER_PORT = 44405
SERVICE_LOGIN_SERVER_NUM = 1
SERVICE_LOGIN_SERVER_PORT = 44405
SERVICE_LOGIN_SERVER_IP_HEAD = connected
SERVICE_LOGIN_SERVER_IP_TAIL = sunclassic.webzen.co.kr
SERVICE_LOGIN_TRY_COUNTS = 10
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
```

**Proof it took effect:** `U2A_askVerify`'s 32-byte host field now carries `127.0.0.1`, not
`connected1.sunc`:

```
33 01 | 07 01 01 | 31 32 37 2E 30 2E 30 2E 31 | 00 × 23      ("127.0.0.1")
```

**Consequence: the hosts entry in §5 is no longer required.** The client dials an IP literal, so
there is no DNS lookup to intercept. Leaving the entry in place is harmless, but it can now be
removed — which also removes the only thing still pointing the client at Webzen's real hostname.

Hashes: pristine `System.wpk` SHA256 `7DE3B134…A76C`; with this `LOGIN.INI`,
`D989FFE45C10B6635AD512B7E2B5F9E832131CFDD895CFC15DA27A7736980604` (size unchanged,
204,564,609). `System.wpk.bak` is verified pristine — restore from it to revert.

Also inside: `System\CLIENT_INFO.INI` → `VERSION = 2.6.0.1`, and `System\PROGRAM.INI`.

---

## 5. Environment changes made (outside the repo)

These live on the machine, not in git.

| change | status | purpose / undo |
|---|---|---|
| **hosts entry** `127.0.0.1 connected1.sunclassic.webzen.co.kr` | **REMOVED 2026-09-01 — no longer needed** | Superseded by `LOGIN_SERVER_TYPE = 1` + `LOGIN_SERVER_IP = 127.0.0.1` (§4), which makes the client dial an IP literal with no DNS lookup to intercept. Verified: with the entry gone the client still connects to `127.0.0.1:44405` and its `askVerify` carries `127.0.0.1`. `connected1.sunclassic.webzen.co.kr` now resolves normally to `125.141.214.85` again. Pre-removal backup: `Tools\Client Analysis\wpk-work\hosts.backup-20260901-222313`. |
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

- **`A2U_ansVerify` format** — §3, the live blocker (but see §10.1: the evidence ruling candidates out is weaker than it looked).
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

  **Where the client parses it: `FUN_005552c3`** (found 2026-09-01, incidentally). It calls
  `CommandLineToArgvW`, converts wide→multibyte, and tokenises on `0x7c` (`'|'`) — i.e. it is the
  decoder for the `42126697|AioHaruka||<token>|2|1|1|2|2` handoff string. It also reads
  `GetModuleFileName` into a `0x104` buffer. When the token format has to be worked out, decompile
  this function rather than guessing: it defines exactly how many `|` fields the client expects
  and what it does with each. Not yet analysed field-by-field.
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
  whether any inbound byte arrives, not on the timing.

  The cursor **persists across restarts** in `Config/AuthServer/AnsVerifyProbe.state` (gitignored,
  a single zero-based index). This matters because Windows locks a running jar, so every rebuild
  forces a restart, and each replayed candidate costs a full ~45s client launch. A fresh cursor
  starts at **candidate 4** — candidates 1–3 have all been served to a live client and failed
  (§3.4). Delete the file to restart the rotation, or write an index into it to jump to a specific
  candidate. The cursor is positional, so **append** to `CANDIDATES` rather than inserting into it.

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

---

## 9. Client reverse engineering — 2026-09-01

### 9.1 `Sungame.exe` is MPRESS-packed; dump it from the live process

Static analysis of `Sungame.exe` is impossible as shipped. Its sections are `.MPRESS1` /
`.MPRESS2` / `.rsrc`, and `.MPRESS1` has entropy **8.000** (the maximum) — the code is compressed.
The import table carries exactly one function per DLL (`WS2_32` → ordinal 19 = `send`); the real
IAT is rebuilt at runtime by the packer stub.

`SUN.exe` is *not* packed and does import full winsock, but it is **not** the login client — it
contains none of the `LOGIN.INI` key strings. Every other client binary is unpacked and
irrelevant (`wzSound`/`binkw32` flag as high-entropy only because of their data sections).

**The working route is a memory dump of the running client.** MPRESS decompresses in-place well
under a second after launch, so the unpacked image is available almost immediately. Confirmed:

- `OpenProcess(PROCESS_VM_READ|PROCESS_QUERY_INFORMATION)` on `Sungame.exe` **succeeds** —
  GameGuard did not block it. The dumper must be **elevated**, because the client is.
- Dumping VA `0x00400000` + `0x13E4000` yields **20,856,832 bytes across 3 regions, zero read
  failures**, reproducibly (two dumps 12s apart were byte-identical in every region checked).
- The result verifies as genuine: code-region entropy drops to **6.644** (normal x86), and the
  `LOGIN.INI` key strings are present.

Dump in **virtual layout** (file offset N == VA `0x400000 + N`) so RVAs index straight in.

**The dump and its tooling are kept outside this repo** (a 20 MB image of a copyrighted binary
does not belong in git) at:

```
Classic\Development\Tools\Client Analysis\
    dump\Sungame-unpacked-2026-09-01.bin     SHA256 3578862...B6D4A71A
    tools\Launch-And-Dump.ps1                run elevated; regenerates a dump in ~30s
    tools\Dump-Image.ps1  PeInfo.java  PeStrings.java  DumpScan.java
    README.md                                load instructions + subcommand reference
```

Two dumps taken 12s apart in the same run hashed identical, so the image is stable once
unpacked. Load into Ghidra as **Raw Binary, x86 / 32-bit / little-endian, base `0x400000`** —
that is where this should continue.

### 9.2 Confirmed at instruction level

The login host composition in §4 is now proven, not inferred. Format string `"%s%d.%s"` lives at
VA `0x00C05144` (exactly one occurrence) and has exactly one xref, at VA `0x0043E393`, which
pushes `IP_HEAD`, `index + 1`, and `IP_TAIL` in that order. So on that path the address really is
`HEAD + <index> + "." + TAIL`, and `IP_HEAD = 127.0.0.1` would compose to `127.0.0.11.`

**But that is only the `LOGIN_SERVER_TYPE == 2` branch.** Decompiling the whole connect routine
`FUN_0043e2be` shows it is gated:

```c
if (*(int *)(cfg + 0x474) == 2) {            // LOGIN_SERVER_TYPE == 2
    count = *(int *)(cfg + 0x4b4);            // SERVICE_LOGIN_SERVER_NUM
    ... build 0..count-1, then 10000 random swaps ...   // shuffles the server order
    do {
        host = FUN_0043c33b("%s%d.%s", IP_HEAD, idx[i] + 1, IP_TAIL);
        port = *(int *)(cfg + 0x4b0);         // SERVICE_LOGIN_SERVER_PORT
        ... store host at +0x4BC, port at +0x4D8 ...
        if (FUN_007a2100(3, c_str(host), port, 2, 1) != 0) break;   // 3 = AUTH slot
    } while (++i < count);
}
// LOGIN_SERVER_TYPE != 2 -> a completely different path:
addr = *(int *)(cfg + 0x470);
FUN_007a2100(3, FUN_00477efa(), addr, 2, 1);
```

So **§4's open question is answered the other way**: `LOGIN_SERVER_TYPE = 1` selects a different
address scheme entirely — a different config field (`+0x470`) and a different host source
(`FUN_00477efa`), with no index-and-dot composition. A direct address *is* possible; the earlier
"a bare IP cannot be given this way" reading came from seeing only the `TYPE == 2` branch in raw
hex and is **wrong**. §4's second and third `LOGIN.INI` attempts (`TYPE = 1`, `IP_HEAD =
127.0.0.1`) were never actually put in front of a client — they are worth retrying.

Also new: the `TYPE == 2` path **randomly shuffles** the server list before trying entries in
order. Invisible with `NUM = 1`, but it means server choice is not deterministic with more.

### 9.3 Map of what has been located

| what | VA |
|---|---|
| winsock IAT block (rebuilt by the stub) | `0x00BF5604` .. `0x00BF563C` |
| `WSASend` / `WSARecv` slots | `0x00BF5604` / `0x00BF5628` |
| `WSASend` / `WSARecv` 7-arg thunks | `0x007FE4C0` / `0x007FE4F0` (one caller each) |
| socket receive method | `0x007FD7F0` (two callers) |
| auth connection manager (connect, disconnect, logging) | `0x0043D700` .. `0x0043E500` |
| its vtable / second vtable | `0x00C0584C` / `0x00C05848` |
| `[Network] AUTHSERVER ...` log strings | `0x00C0514C`, `0x00C05180`, `0x00C051B4`, `0x00C052EC` |

### 9.4a Ghidra is set up — reuse it, do not redo it

Ghidra 12.1.3 is installed at `Classic\Development\Tools\ghidra_12.1.3_PUBLIC` and the dump is
**already imported and fully analysed** (38,212 functions recovered) in
`Client Analysis\ghidra-project\SungameClassic`. Analysis took ~7 minutes and does not need
repeating — `tools\ghidra\Run-Headless.ps1` passes `-process ... -noanalysis` on every later run,
which starts in seconds.

**It must run on JDK 21, not 25.** Ghidra 12.1.3 bundles Apache Felix 7.0.5, whose
`handleJavaVersionChange()` throws `NullPointerException: dataFile is null` ("The data file must
be inside the data dir") on JDK 25 and kills headless before any analysis. `Run-Headless.ps1`
pins `C:\Program Files\Java\jdk-21`. If it ever fails that way again, also delete
`%APPDATA%\ghidra\ghidra_12.1.3_PUBLIC\osgi`.

Scripts in `tools\ghidra\`: `DecompileAt` (decompile a list of VAs), `CallersUp` (upward call
graph), `FindDispatch` (every computed jump with its true case count, from Ghidra's recovered
switch data), `FindCmpConst` (functions containing a real `CMP` against given constants),
`Find-And-Decompile.ps1` (chains FindDispatch into DecompileAt).

### 9.4 NOT found — the opcode dispatch

The handler for `A2U_ansVerify` was **not** located, by byte scanning *or* by full Ghidra
analysis. The negative results are recorded so they are not repeated:

**Exhaustive scans that came back empty.**

- **All 175 computed jumps with >= 8 cases** (Ghidra's recovered switch targets, so case counts
  are exact). None dispatches AUTH opcodes. The best-ranked, `FUN_00544746` (61 cases, and it
  references 0x33/0x0E/0x1A), is an object-construction loop — `operator new(size)` per case
  storing into `[this + 0x102a0 + 4i]`.
- **All 382 functions containing a real `CMP` against the AUTH opcode set.** The three best are
  all false positives: `FUN_008049d0` matched every constant because it tests a *contiguous* run
  `0x0d..0x14, 0x1a, ... 0x31,0x32,0x33` (character classification); `FUN_004c5a34` compares
  equipment-slot indices (`FUN_008053f0(0xd/0xe/0xf)`, item id `0x374`); `FUN_004c5cb5`'s table is
  an enum→value map.
- **The +0x102A0 registry** (61 objects, accessor `FUN_0040f760`, bounds-checked
  `[ecx + eax*4 + 0x102A0]`) looked like a handler table but its callers are UI paths — it is a
  UI window registry.

**Structural facts established.**

- The auth vtable at `0x00C0584C` belongs to the **login-screen UI class**, not a network class:
  its methods compare against `0x100` (`WM_KEYDOWN`), `0x0d` (`VK_RETURN`), `0x1c`, `0x1f`, `0x20`.
  It owns the connect routine only because the login screen initiates the connection.
- The receive path is a **generic overlapped-I/O engine**, not game code:
  `FUN_007fe290` (worker; `WaitForMultipleObjects` / `GetOverlappedResult`) → `FUN_007fe000` →
  `FUN_007fd7f0` → `WSARecv` thunk `0x007FE4F0`. Connect and receive meet at `FUN_007a2100`.
  Completed reads reach game code through a virtual call, which is why no opcode constant appears
  anywhere along this path.

**Conclusion:** the client does not dispatch AUTH packets by a switch on the opcode, nor by
literal comparison against `0x33`/`0x02`, in any form these scans can see. The dispatch is
indirect — a virtual call on a per-connection handler object. Finding it needs the *dynamic*
approach: breakpoint the `WSARecv` thunk at `0x007FE4F0` (or `FUN_007fd7f0`) in a debugger and
step the return path once, with a live `A2U_ansReady` in the buffer. That reads the answer off
the running client in one pass instead of inferring it statically. OllyDBG 1.10 + OllyDump are
already in `Tools\`; GameGuard tolerated memory reads, but whether it tolerates a debugger is
untested.

Earlier ruled out (byte-scan era), kept so it is not re-searched:

- The auth connection-manager module contains **no** jump table bounded at `0x1A` and **no**
  comparison against any of `0x1A`, `0x0E`, `0x02` — it handles connect/disconnect/logging only,
  not packet parsing.
- Of 316 jump tables in the image, the two with 27 entries and bound `0x1A` are both something
  else. `0x004C5CB5`'s targets are a 340-byte cluster (an enum→value map). `0x0069B2D1` is guarded
  by `[esi+1]==0x27 && [esi+2]==0x04` and indexes on `[esi+4] - 1`, so it is a different
  subsystem, not category `0x33`.
- No `mov word ptr [...], 0133h`, so the `33 01` header is assembled via a helper rather than
  written as a constant.

Finishing this needs a real disassembler on the dump (Ghidra is free and handles a 20 MB flat
image fine, loaded as raw x86-32 at base `0x400000`). Hexdump archaeology got as far as it
usefully can.

### 9.5 Hypothesis this raises: the reply opcode itself may be wrong

`A2U_ansVerify = 0x02` comes from the **old server code, not from any capture.** The client has
confirmed `0x00` (it accepts our `A2U_ansReady`) and `0x01` (it sends `U2A_askVerify`) — it has
never confirmed the opcode of the answer.

Six candidates (§3.4) with bodies of 0, 1, 2, 4, 8 and 35 bytes all failed *identically*: not one
inbound byte, no progression. A body-shape problem would not normally look that uniform. An
**opcode** the client does not recognise would — the packet is dropped before the body is ever
examined.

**DISPROVED 2026-09-03 — see §14.1.** The sweep was run: all 32 opcodes `0x00`-`0x1F` with a `00`
body, served individually to a live client. None is accepted as a verify reply. Only `0x0E` reacts,
and it reacts as `A2U_ansAuthUser` (§14.2), driving the client to `U2A_askSrvList` without any
verify having succeeded. The opcode was not the wrong variable, and `A2U_ansVerify = 0x02` is not
contradicted by anything — see §14.3 for what that leaves.

### 9.6 The old `Ashime/LoginServer` repo — what it does and does not settle

Checked against <https://github.com/Ashime/LoginServer> (the project our packet classes were
ported from). It cannot supply the Classic `ansVerify` format, but it settles several things.

**Its `ansVerify` is byte-identical to ours — it is the *source* of our implementation, not
independent evidence.** `AnsVerifyPacket.createPacket` → `MessageEncoder.createShortPacket(0x33,
0x02, result)` → `Convert.intToByteArray` (2 bytes, big-endian) → `Utility.flip(0,1)`. Wire:
`03 00 | 33 02 00`. That is exactly the `0300330200` our `ClientPacketEncoder` logs today.

**PARTLY WITHDRAWN 2026-09-03 — see §11.** The EP1 packet documentation shows the `askVerify`
*layout* is identical across both generations; what follows read a difference into `verify()`, which
was broken code rather than evidence of one. The `askAuthUser` layout genuinely did change (see
`Protocol.java`), so the generations do differ — just not here.

**It targeted a different client generation — this is the important finding.** Its
`VerifyIpProtocol.verify()` tests the askVerify payload for the *client protocol number* and the
*server IP*:

```java
String serverIP = IniFile.getConnectionIP().replaceAll(".", "");
String clientProtocol = IniFile.getProtocol().replaceAll(".", "");
return hexPacket.contains(clientProtocol) && hexPacket.contains(serverIP);
```

The Classic 2.6.0.1 client's `U2A_askVerify` contains **neither** — it carries `07 01 01` plus a
32-byte hostname (§2). Two different request layouts means two different client generations, so
there is no reason to expect the *response* layout to have survived either. This both explains
why `33 02 00` fails here despite that older stack reaching Channel Select, and undercuts the
assumption that the Classic reply is a 3-byte result at opcode `0x02` at all (§9.5).

**That `verify()` also never actually validated anything.** `replaceAll(".", "")` uses an
unescaped `.`, which matches *every* character — both strings collapse to `""`, and
`contains("")` is always true. It always returned true, so the old server always replied `0x00`.
Its success tells us only that an EP1-era client accepted result byte `0x00`; the comparison our
`VerifyUser` originally ported was dead code in the original too.

**What it does confirm:**

- **§1 independently.** It also puts a 2-byte **little-endian** length on the wire (big-endian
  bytes, then `flip(0,1)`). Two unrelated implementations agree — the LE finding is solid.
- `Category.LOGIN = 0x33`, matching `Category.AUTH`.
- The TEA key in `ansReady` is **4 bytes** (`SessionHandler`/`Tea.generateKey`), as we send.
- **Nothing is encrypted at the verify stage** — `MessageEncoder` has no cipher path at all. This
  does not rule out encryption in the Classic client, but there is no precedent for it here.

**Still worth mining later:** `AnsAuthUser`, `AnsServerList`, `AnsServerSelect` and `Tea.java`,
once verify is solved — with the same caveat that they are EP1-era layouts.

---

## 10. The launcher was competing with the game client on the C2S listener — 2026-09-03

Symptom that started this: *"I cannot tell if the client is sending a packet back or not due to the
launcher's heartbeat check."* Two separate causes, both now fixed.

### 10.1 `UniqueIpFilter` let only one 127.0.0.1 connection exist at a time

`NioServer#initC2S` added Netty's `UniqueIpFilter` **unconditionally**, even though
`AuthServer.ini` carries `[NETWORK] UNIQUE_IP_FILTER` and `AuthServerConfig` has always read it
into a field. Setting the flag `FALSE` did nothing; the filter was always on.

That filter allows one connection **per remote IP**. On this machine everything is `127.0.0.1`:

| who | connection |
|---|---|
| launcher `NioClient` | persistent, opened at launcher startup, **deliberately held open across the handoff** (`LauncherController#onStartGame`: *"the launcher is the only thing holding the AuthServer connection"*) |
| launcher `ServerHealthCheck` | a fresh TCP connect **every 5 seconds** (`AUTH_SERVER_CHECK_INTERVAL_SECONDS`) |
| game client | its own connection to the same `44405` |

So with the launcher open, it owns `127.0.0.1` and the game client is closed the instant it is
accepted. Netty's `AbstractRemoteAddressFilter` still fires `channelActive` downstream on a
rejected channel before closing it.

> **This was originally written as an explanation for §3.3's "connect #1 → closes immediately,
> sends nothing". §12.1 disproves that** — the 2026-09-03 run reproduced the phantom connection
> with the filter off and no launcher running, so it is genuine client behaviour. The collision
> described here is still a real defect, but it has been reasoned from Netty semantics, not
> observed.

**Fixed:** the filter is now gated on `AuthServerConfig.isUniqueIpFilter()`, and
`AuthServer.ini` sets `UNIQUE_IP_FILTER = FALSE` with a comment saying why. Keep it `FALSE` for
any local client testing.

**What this does and does not invalidate in §3.** It does *not* invalidate the runs where a
`U2A_askVerify` was actually received — a blocked client never gets that far, so those exchanges
happened on a connection that was let through. It *does* mean that any **second** connection the
client opened after our reply would have been killed silently, and "no inbound byte, no
progression" was the only signal §3.4 ranked candidates on. Re-run the controls with the filter
off before treating any candidate as ruled out.

`database-server`'s S2S listener has the same unconditional `UniqueIpFilter` and no ini flag at
all. Harmless today with one S2S client, but auth-server + game-server + web-server all connecting
from `127.0.0.1` will collide the same way. Not changed here.

### 10.2 The heartbeat made real client traffic unreadable

Every 5 seconds `ServerHealthCheck.isReachable` connects and closes without sending a byte.
`ClientPacketHandler` logged `Client connected from ...` / `Client at ... disconnected.` at INFO
for each one, with nothing to distinguish it from the game client — hence not being able to tell
what the client did.

**Fixed** in `ClientPacketHandler`:

- every line carries a `[conn-N]` tag, so one connection's exchange can be followed end to end;
- `channelActive` and a zero-packet close log at **DEBUG**, so heartbeats disappear from an INFO
  log entirely;
- the first inbound packet logs at **INFO** (`this connection is a real client, not a
  health-check probe`), and every packet logs as `[conn-N] <- #k <hex>`;
- a close that received anything logs `[conn-N] ... disconnected after Nms having sent K packet(s)`.

So the original question is now read straight off the log: find the `[conn-N]` that logged
`<- #1 3301...`, and see whether that same `conn-N` ever logs `<- #2`. The close line states the
total either way.

Simplest alternative if the log still needs to be quieter: **close the launcher before launching
the client.** It is only needed to start the client, and its connection is a placeholder for a real
handoff token anyway (§7).

---

## 11. EP1 `ClientPackets` / `ServerPackets` documentation — 2026-09-03

Two PDFs from the earlier EP1 LoginServer project. They are **documentation of the EP1 protocol**,
not a Classic capture, so they are corroboration rather than proof — but they line up with the live
Classic captures far more than §9.6 assumed.

### 11.1 Every opcode matches `Protocol.java`

`ansReady 0x00`, `askVerify 0x01`, `ansVerify 0x02`, `askAuthUser 0x03`, `ansAuthUser 0x0E`,
`askSrvList 0x0F`, `ansSrvList_Srv 0x11`, `ansSrvList_Chn 0x12`, `askSrvSelect 0x13` — all identical
to ours. `ansServerSelect` is documented **without** opcode values, so `A2U_ansSrvSelect = 0x1A` is
still unconfirmed by anything.

This weakens **§9.5** (the "maybe the reply opcode is wrong" hypothesis) but does not kill it: the
documented `0x02` is EP1's, and the Classic client has still never confirmed it.

### 11.2 `askVerify` is unchanged between generations — §9.6 was reading a bug, not a difference

```
askVerify   Size(2) | Category 0x33 | Protocol 0x01 | ClientProtocol(3) | ServerConnectionIP(32)
```

That is the §2 capture byte for byte. §9.6 concluded "two different request layouts means two
different client generations"; it reached that from the old repo's `VerifyIpProtocol.verify()`,
which §9.6 itself then shows was dead code (`replaceAll(".", "")` collapses both operands to `""`).
The layout never differed — see the correction now in §3.1.

`askAuthUser` **did** change, so the generations really are different, just not here:

| | EP1 doc | Classic capture (`Protocol.java`) |
|---|---|---|
| body | `0x00 x4` \| user 50 \| pass 16 \| unknown 8 | len `08` \| `00 x3` \| UID 8 \| `00` \| user 40 \| `00` \| pass 36 \| `00` |
| total | 82 bytes | 93 bytes (`5d 00`) |

The EP1 note that the trailing 8 bytes are *constant across sessions* is worth keeping: in Classic
that region is the launcher-supplied UID, which would behave the same way.

### 11.3 `ansVerify` is the 3-byte form we already send

```
ansVerify   Size(2) | 0x33 | 0x02 | VerifyFlag(1)   0x00 = true, 0x01 = false
```

Identical to `AnsVerify.createPacket`, and it settles the flag polarity (which §3.4 had already
shown was not the blocker). It does not explain the Classic client's silence — but combined with
§10.1, "the reply is wrong" is no longer the only explanation on the table.

### 11.4 New material for the stubs after verify

`AnsSrvList` and `AnsSrvSelect` are currently hardcoded zero-entry placeholders. The docs give real
layouts to build against — EP1-era, so verify them against a capture before trusting them:

```
ansServerList (0x11)   Size(2) | 0x33 | 0x11 | count(1)
                       then per server: Name(32) | Unknown(1) | Server#(1) | Unknown(1)
                       entries separated by a single 0x00

ansChannelList (0x12)  Size(2) | 0x33 | 0x12 | count(1)
                       then per channel: Name(33) | Server#(1) | Channel#(1) | Terminator(1, MUST NOT be 0x00)
                       entries separated by a single 0x00

ansServerSelect        Size(2) | Category | Protocol | Unknown1(36, 0x00)
                       | ConnectionIP(32) | ConnectionPort(2) | Unknown2(3, 0x00)
```

Note the asymmetry that is easy to get wrong: server names are **32** bytes with no terminator
byte, channel names are **33** and are followed by a terminator that must be non-zero. The
separator between entries is a single `0x00` in both, and there is no separator after the last
entry.

`askServerSelect` is documented as `Size(1 bytes) | 0x33 | 0x13 | Server#(1) | Channel#(1)` — the
size field is 2 bytes everywhere else in both documents, so read that as a typo.

`ansServerSelect` is the packet that hands the client off to the game server, so its
`ConnectionIP`/`ConnectionPort` is where **§7's Channel Select → GameServer step** gets its
address. Worth revisiting the moment verify is unblocked.

Source PDFs: `ClientPackets.pdf` / `ServerPackets.pdf` (user's Downloads folder, not in the repo).

---

## 12. Test run 2026-09-03 20:16 — the client sends nothing after `ansVerify`, confirmed

First run on the §10 logging. Rebuilt (`mvnw clean install -DskipTests`, BUILD SUCCESS),
database-server then auth-server from the repo root, healthy startup chain
(`Server successfully connected to 127.0.0.1:10000` → `TLS handshake completed` →
`Received ServerInfo response`). **Launcher not running**, `UNIQUE_IP_FILTER = FALSE`.
Client started directly: `Sungame.exe -User:AioHaruka -Password:<capture token>`, elevated.

```
20:16:57.236  conn-1  ->  0600330000B01731     A2U_ansReady           (client sent nothing back)
20:17:12.636  conn-2  ->  060033000005CC9F     A2U_ansReady
20:17:12.642  conn-2  <-  #1 33 01 07 01 01 "127.0.0.1" 00×23        U2A_askVerify
20:17:12.644  conn-2  ->  0300330200           A2U_ansVerify  33 02 00
20:17:14.023  conn-2      disconnected after 1387ms having sent 1 packet(s)
```

Then `Sungame.exe` exits. So, stated plainly and no longer inferred from log noise:

**The client sends exactly one packet, and closes ~1.4s after our `33 02 00` without sending a
second byte.** That is what the `having sent 1 packet(s)` close line is for — it is now a direct
measurement rather than something read out of an ambiguous log.

`VerifyUser` also passed both of its checks against a live client for the first time:

```
askVerify from a client running protocol 7.1.1, dialled '127.0.0.1'.
```

So `CLIENT_PROTOCOL = 7.1.1` and `LOGIN_SERVER_TYPE = 1` are both correct, the §11.2 field mapping
holds against the real client, and the reply really was the documented accept (`0x00`), not a
refusal.

### 12.1 CORRECTION to §10.1 — the phantom "connect #1" is genuine client behaviour

§10.1 guessed that §3.3's *"connect #1 → closes immediately, sends nothing"* was `UniqueIpFilter`
rejecting the client. **That guess is wrong.** This run had the filter off and no launcher running,
and connect #1 still happened exactly as described — `conn-1` received its `A2U_ansReady` and sent
nothing, and the real attempt came 15.4 seconds later on `conn-2`.

The client opens a throwaway connection first. Reason unknown; a reachability check of its own is
the obvious guess. Rule it out as an explanation for anything.

The rest of §10.1 stands: the filter really was unconditional despite the ini flag, and the launcher
really does hold a `127.0.0.1` connection open across the handoff. That the two collide is still a
sound deduction from Netty's `AbstractRemoteAddressFilter` semantics, but note it has **not** been
observed directly — only the fix has been made.

### 12.2 New timing row for §3.2

| when | response | close after |
|---|---|---|
| 2026-09-03 20:17 | `33 02 00`, verify checks passing | **1.4s** |

Fastest close recorded for these three bytes, against 3.8s / 14.8s / 5.2s previously. Consistent
with §3.2's finding that close-delay is not a function of what we send — do not read anything into
it.

### 12.3 Where this leaves the blocker

Every alternative explanation that §10 raised is now closed off. The client received a
well-formed, documented-shape, accepting `A2U_ansVerify` on a connection nothing else was
competing for, with both request fields validating, and it still closed without a word.

The remaining live hypotheses are unchanged and both point the same way:

- **§9.5** — the reply *opcode* may not be `0x02` for this client generation. §11.1 shows `0x02`
  is EP1's documented value, and the Classic client has never confirmed it. Cheaper to sweep than
  the body, and `AnsVerifyProbe` can do it by appending opcode-varying candidates.
- **§9.4** — read the answer off the running client instead of guessing: breakpoint the `WSARecv`
  thunk at `0x007FE4F0` and step the return path once with a live reply in the buffer. Ghidra is
  already set up (§9.4a) and the dump is already analysed.

The §9.4 route is the one that ends this. Everything else is a sweep over a space that seven
candidates have now failed to find.

---

## 13. BREAKTHROUGH 2026-09-03 20:45 — the client got past verify

The opcode sweep (§9.5) hit on its first real launch. `AnsVerifyProbe` was rebuilt to vary the
*opcode* with a fixed `00` body, eight opcodes per connection (see its class comment), cursor set to
candidate 11. One client launch, one connection:

```
20:45:08.985  conn-2  <- #1  33 01 07 01 01 "127.0.0.1" 00×23      U2A_askVerify
20:45:08.993  conn-2  ->     33 00 00  33 01 00  33 02 00  33 03 00
                             33 04 00  33 05 00  33 06 00  33 07 00   candidate 11 (0x00-0x07)
20:45:08.996  conn-2  <- #2  33 01 07 01 01 "127.0.0.1" 00×23      U2A_askVerify AGAIN
20:45:08.996  conn-2  ->     33 08 00  33 09 00  33 0A 00  33 0B 00
                             33 0C 00  33 0D 00  33 0E 00  33 0F 00   candidate 12 (0x08-0x0F)
20:45:09.000  conn-2  <- #3  33 0F                                 U2A_askSrvList  <-- NEW
20:45:09.000              (dropped: "asked for the server list before authenticating")
20:45:13.666  conn-2         disconnected after 4688ms having sent 3 packet(s)
```

Three inbound packets where every previous run in this project produced exactly one. **This is the
furthest the client has ever gone.**

### 13.1 `SERVICE_LOGIN_TRY_COUNTS` / "one askVerify per launch" is dead — §3.3 is obsolete

§3.3 recorded that the client makes exactly one `U2A_askVerify` per launch and then exits, which is
why every candidate cost a full ~2-3 minute launch. **That is not a client limitation.** Packet #2
above is a second `U2A_askVerify` on the *same connection*, seconds after the first, and the probe
advanced two candidates in one launch as a result.

The trigger is almost certainly `33 00 00` — opcode `0x00` is `A2U_ansReady`, and re-sending it
restarts the handshake. (Note it worked as a 3-byte packet, where the real `ansReady` is `33 00` +
a 4-byte TEA key.)

**Consequence: the whole sweep can run inside a single launch** by appending a `33 00 00`
re-trigger after each candidate, so the client re-verifies and pulls the next one. Budgeting one
launch per candidate is no longer necessary.

### 13.2 What actually caused the progression is not yet isolated

Burst 2 is `0x08`..`0x0F`, and two of those are already spoken for in our map:

- **`0x0E` = `A2U_ansAuthUser`.** We sent `33 0E 00`, which is a documented *authentication
  succeeded*. If the client accepted that, it would jump to asking for the server list regardless
  of what it made of verify — which is exactly what packet #3 is.
- **`0x0F` = `U2A_askSrvList`**, a client→server opcode; sending it back is noise.

So there are two live readings, and this run cannot tell them apart:

1. one of `0x08`..`0x0D` is the real `A2U_ansVerify`, the client accepted it, and then `0x0E`
   carried it through auth as well; or
2. no ansVerify was accepted at all, and `0x0E` alone short-circuited the client straight to
   `askSrvList`.

Reading 2 is entirely possible and would mean verify is still unsolved. **Isolate before
concluding anything.** The obvious first cut is to serve `0x08`..`0x0D` *without* `0x0E`: if the
client still reaches `askSrvList`, verify is genuinely solved and reading 1 holds; if it goes quiet
again, `0x0E` was doing all the work.

Single-opcode candidates are laid out for exactly this — candidate number is `15 + opcode`, so
`0x08` is candidate 23 through `0x0F` at candidate 30.

### 13.3 `askSrvList` matches the EP1 documentation exactly

Packet #3 is `33 0F` — two bytes, no body. The EP1 `ClientPackets` PDF (§11) specifies
`askServerList` as `Size(2) | Category 0x33 | Protocol 0x0F` and nothing else. First confirmation
of a §11 layout against the Classic client beyond `askVerify`, and more reason to trust §11.4's
`ansServerList` / `ansChannelList` / `ansServerSelect` layouts when they are built out.

### 13.4 Two things now block the next step

Both are in our code, not the client's:

- **`AnsSrvList` is a hardcoded zero-entry placeholder** (`33 11 00` / `33 12 00`). §11.4 has real
  layouts to build against, and there is still no S2S query for auth-server to fetch live
  `ServerInfo` rows from database-server.
- **`ClientPacketHandler` drops `askSrvList` when `!session.isAuthenticated()`**, which is what
  happened here — the client reached the server list without ever sending `U2A_askAuthUser`. That
  gate is correct for production and wrong for probing. Either the client genuinely does not
  authenticate at this point (in which case the gate is simply wrong), or it was short-circuited by
  our stray `0x0E` (§13.2). Resolve §13.2 first; the answer decides which.

---

## 14. Isolation run 2026-09-03 20:50 — `0x0E` did it, and the opcode sweep is complete

The re-trigger from §13.1 was built into `AnsVerifyProbe` (each opcode-sweep candidate is followed
by `33 00 00`, bounded by `ClientPacketHandler`'s `MAX_PROBE_RETRIGGERS`). Cursor set to candidate
23 (`0x08`). **One client launch swept every remaining opcode — 26 inbound packets in 8.7 seconds**,
where the entire project had previously managed one per launch.

```
<- #1   askVerify        cand 23  opcode 0x08  ->  askVerify
<- #2   askVerify        cand 24  opcode 0x09  ->  askVerify
<- #3   askVerify        cand 25  opcode 0x0A  ->  askVerify
<- #4   askVerify        cand 26  opcode 0x0B  ->  askVerify
<- #5   askVerify        cand 27  opcode 0x0C  ->  askVerify
<- #6   askVerify        cand 28  opcode 0x0D  ->  askVerify
<- #7   askVerify        cand 29  opcode 0x0E  ->  #8 = 33 0F   U2A_askSrvList   <-- the only hit
<- #9 .. #26  askVerify  cand 30-46 opcodes 0x0F..0x1F, then wrapped to cand 1 (33 02 00)
        disconnected after 8770ms having sent 26 packet(s)
```

Every `askVerify` above is the re-trigger doing its job, not the candidate. Only one candidate ever
produced anything else.

### 14.1 Reading 2 of §13.2 is confirmed — no opcode is accepted as `ansVerify`

`0x08`..`0x0D` were served individually, each with `0x0E` nowhere in play, and the client answered
every one with nothing but another `askVerify`. Then `0x0E` alone, on its own, produced
`U2A_askSrvList` immediately.

**So `0x0E` short-circuits the client straight to the server list, and there is no verify reply in
`0x00`..`0x1F` at all** (with body `00`). Combined with §3.4's seven body shapes at `0x02`, the
"the reply we send is wrong" line of attack is now close to exhausted:

| swept | result |
|---|---|
| 7 body shapes at opcode `0x02` (§3.4) | nothing |
| all 32 opcodes `0x00`-`0x1F`, body `00` (§13, §14) | nothing except `0x0E` |

§9.5's hypothesis - that the opcode rather than the body was the wrong variable - is therefore
**disproved**. `A2U_ansVerify = 0x02` is not contradicted by anything; it simply does not produce a
visible reaction.

### 14.2 CONFIRMED against the Classic client: `A2U_ansAuthUser = 0x0E`, `0x00` = success

`33 0E 00` is accepted and acted upon. This is the **first server→client packet this project has
ever had confirmed by client behaviour** beyond `A2U_ansReady`, and it validates the EP1
documentation (§11.1) on the Classic client: opcode `0x0E`, one-byte flag, `0x00` = authenticated.

It also shows the client does **not** run a strict state machine over this connection - it acted on
`ansAuthUser` having never sent `U2A_askAuthUser`, and having never had a verify reply it accepted.

### 14.3 This reframes the blocker: verify may not gate anything

The working assumption for this whole project has been that `A2U_ansVerify` is a gate and nothing
can proceed until it is right. That is now doubtful. The client reached `U2A_askSrvList` - the
screen after login - with verify never satisfied.

The likeliest reading is that `askVerify` is informational (the client reporting its protocol
version and the address it dialled, §11.2) and that whatever `ansVerify` does, it does not produce
an outbound packet. Everything read as "the client rejected our reply" may only ever have been the
client sitting on the login screen waiting for input, then timing out. Note the close delay for the
identical `33 02 00` has now been 3.8s, 14.8s, 5.2s, 1.4s and 8.7s across runs (§3.2, §12.2) - not
the signature of a protocol rejection.

**The route forward is to stop trying to satisfy verify and push on from `askSrvList` instead**,
which `0x0E` reaches on demand. That is also the direct path to §7's real objective, the Channel
Select → GameServer handoff.

Blockers for that, both ours (§13.4):

- `AnsSrvList` is a zero-entry placeholder; §11.4 has the EP1 layouts to build against, and there
  is no S2S query yet for auth-server to fetch live `ServerInfo` rows.
- `ClientPacketHandler` drops `askSrvList` when `!session.isAuthenticated()`. Given §14.2, the
  client can arrive there without ever authenticating, so for probing that gate has to be relaxed -
  or `session.setAuthenticated(true)` set when an `ansAuthUser` success is sent.

### 14.4 The re-trigger changes the economics permanently

A launch now costs one candidate list, not one candidate. Anything worth sweeping - bodies at a
fixed opcode, category bytes, longer payloads - is now cheap. `MAX_PROBE_RETRIGGERS = 40` bounds a
connection; raise it if a list outgrows that.

---

## 15. `AnsSrvList` built to the §11.4 layouts; auth gate relaxed under the probe — 2026-09-03

Both blockers from §13.4 are cleared. **Verified byte-for-byte against a synthetic client; not yet
put in front of the real one** (the elevation prompt for the client launch was declined, so that
run is still outstanding).

### 15.1 What was built

**`AnsSrvList`** now emits the §11.4 layouts instead of `33 11 00` / `33 12 00` placeholders:

```
Servers  0x33 | 0x11 | count(1) | entry [ 0x00 entry ]*
         entry = Name(32, null-padded) | Unknown(1) | Server#(1) | Unknown(1)

Channels 0x33 | 0x12 | count(1) | entry [ 0x00 entry ]*
         entry = Name(33, null-padded) | Server#(1) | Channel#(1) | Terminator(1, NOT 0x00)
```

Observed on the wire, driven by a synthetic `33 0F`:

```
len=38  33 11 01 47 41 4D 45 20 53 45 52 56 45 52 00×21 | 00 00 00      "GAME SERVER"
len=39  33 12 01 43 68 61 6E 6E 65 6C 20 31 00×24 | 00 00 01           "Channel 1"
```

38 = 2 + 1 + 32 + 3 and 39 = 2 + 1 + 33 + 3, so the 32/33 asymmetry is carried correctly.

**`GameServerRegistry`** (new, `network/session/server/`) holds the `ServerInfo` row auth-server
already receives during the S2S handshake and previously only logged — `serverID=7`,
`GAME SERVER`, `127.0.0.1:10002`. So the server list is **live data**, not a hardcoded string.

Channels are **not** live: nothing in this stack models them (no table, no config, no S2S query),
so one `"Channel 1"` is synthesised per server purely so there is something selectable. Listing
more than one server needs a real S2S "list servers" query — `ServerInfoDAO#getServerInfo` exists
on the database-server side but nothing exposes it over the wire.

### 15.2 Three bytes are guesses, and they are the likeliest thing to be wrong

The documentation gives no values for the server entry's two "Unknown (1 byte)" fields. Both are
written as `0x00`, the literal reading.

**If the client rejects the server list, `SERVER_ENTRY_TERMINATOR` is the first thing to change.**
The channel entry has a byte in the same trailing position that the documentation explicitly says
*cannot* be `0x00`. If the two fields are the same thing, the server entry's should be `0x01` too.
Both are named constants in `AnsSrvList` — it is a one-line flip.

Server and channel numbers are handed out **zero-based**, on the assumption that they are indexes
rather than display numbers. Unconfirmed. The client echoes both back in `U2A_askSrvSelect`, which
`ClientPacketHandler` logs, so the first real selection settles it.

### 15.3 The auth gate

`ClientPacketHandler` dropped `askSrvList`/`askSrvSelect` unless `session.isAuthenticated()`, which
rejected exactly the traffic the probe exists to produce: `0x0E` makes the client skip
`U2A_askAuthUser` entirely (§14.2), so the session never learns it is authenticated.

Both checks now go through `isAllowed(...)`, which serves the packet anyway **while
`ANS_VERIFY_PROBE` is on**, logging `PROBE: ... asked for ... without authenticating - answering
anyway`. Tied to the probe flag rather than a setting of its own because the two are inseparable:
without the probe the client never gets past verify, so it never arrives here unauthenticated.
Production behaviour is unchanged, since the probe is off there.

### 15.4 New probe candidate 47 — the `askSrvList` driver

`0x0E` with **no re-trigger**. The sweep candidates all append `33 00 00`, which would keep pulling
the client back to `askVerify` while it is trying to work through the server list. Candidate 47
sends `33 0E 00` and then leaves the client alone.

`AuthServer.ini` is set to it (`ANS_VERIFY_PROBE = TRUE`, cursor `46`), so the next client launch
goes: `askVerify` → `33 0E 00` → `askSrvList` → the real list above → whatever the client does next.

### 15.5 Outstanding

- **The real-client run.** Everything above is confirmed against a synthetic client only.
- **`AnsSrvSelect` is still a 3-byte placeholder** (`33 1A 00`). §11.4 documents it as
  `Unknown1(36, 0x00) | ConnectionIP(32) | ConnectionPort(2) | Unknown2(3, 0x00)`, and its
  `ConnectionIP`/`ConnectionPort` is where the client gets the game-server address — i.e. this is
  the actual §7 Channel Select → GameServer handoff. `GameServerRegistry` already holds the
  `127.0.0.1:10002` it needs. This is the obvious next build.
- **`A2U_ansSrvSelect = 0x1A` is unconfirmed** — §11.1 notes the EP1 documentation gives
  `ansServerSelect` without opcode values, so unlike every other opcode it has no corroboration at
  all. If the handoff packet is ignored, sweep this the way §14 swept verify; the harness now makes
  that cheap.

---

## 16. The client reached Server Select and picked a server — 2026-09-03 21:44

§15's `AnsSrvList` put in front of the real client. **It was accepted.** The client parsed both
list packets and made a selection.

```
21:44:38.310  conn-2  <- #1  33 01 07 01 01 "127.0.0.1" 00×23   U2A_askVerify
21:44:38.316  conn-2  ->     33 0E 00                           probe candidate 47
21:44:38.318  conn-2  <- #2  33 0F                              U2A_askSrvList
21:44:38.318  conn-2  ->     33 11 01 "GAME SERVER"… (38 bytes)  A2U_ansSrvList_Srv
                             33 12 01 "Channel 1"…   (39 bytes)  A2U_ansSrvList_Chn
       ... 7.1 seconds ...
21:44:45.466  conn-2  <- #3  33 13 00 00                        U2A_askSrvSelect  <-- NEW
21:44:45.468  conn-2  ->     33 1A 00                           A2U_ansSrvSelect (placeholder)
21:44:46.956  conn-2         disconnected after 8653ms having sent 3 packet(s)
21:44:46.960  conn-3  <- #1  askVerify   (client reconnected, got 33 02 00, closed, exited)
```

The **7.1-second gap** between the list going out and the selection coming back is the tell: that
is a screen being displayed and a choice being made, not a protocol reflex. The client got as far
as Server Select.

### 16.1 What §15's guesses turned out to be

All three unknowns from §15.2 are settled, and none of them needed changing:

| guess | outcome |
|---|---|
| `SERVER_NAME_TERMINATOR = 0x00` | correct |
| `SERVER_ENTRY_TERMINATOR = 0x00` | **correct** - the `0x01` fallback in §15.2 is not needed |
| zero-based server/channel numbers | correct - the client echoed back exactly the `00`/`00` it was given |

So the §11.4 `ansServerList` / `ansChannelList` layouts hold against the Classic client, including
the 32-vs-33 name-width asymmetry. That is the third and fourth EP1 layout to survive contact
(after `askVerify` §11.2 and `askSrvList` §13.3).

### 16.2 `U2A_askSrvSelect` confirmed, and the documented size typo confirmed too

`33 13 00 00` — category, opcode `0x13`, server `0x00`, channel `0x00`. Exactly §11.4's
`askServerSelect`. The frame carried a **2-byte** little-endian length header like everything else,
so the documentation's `Size (1 bytes)` on that packet really is a typo, as §11.4 assumed.

### 16.3 The blocker is now `A2U_ansSrvSelect` — and this is §7's handoff

The client was answered with the 3-byte placeholder `33 1A 00`, closed the connection 1.5s later,
reconnected once, and exited. So the placeholder is not accepted — unsurprising, since §11.4
documents a 73-byte body:

```
ansServerSelect   Size(2) | Category | Protocol | Unknown1(36, 0x00)
                  | ConnectionIP(32) | ConnectionPort(2) | Unknown2(3, 0x00)
```

**This is the packet that hands the client to the game server**, so building it is §7's Channel
Select → GameServer step directly. `GameServerRegistry` already holds what it needs -
`127.0.0.1:10002`, from the live `ServerInfo` row.

Two unknowns to be aware of when it fails:

- **`A2U_ansSrvSelect = 0x1A` has no corroboration at all.** §11.1: the EP1 documentation gives
  `ansServerSelect` without opcode values, so unlike every other opcode in the map this one comes
  only from the old server code. If a well-formed 73-byte body is still ignored, sweep the opcode
  the way §14 swept verify — the re-trigger harness (§14.4) makes that cheap.
- **Port byte order.** Everything else on this wire is little-endian (§1), but `ConnectionPort` is
  a 2-byte field inside a body, not a length header, so it is worth trying both.

### 16.4 Reproducing this run

`ANS_VERIFY_PROBE = TRUE` with the cursor at **46** (candidate 47, the `0x0E` driver). The cursor
wraps after each launch, so **reset it to 46** before each run or the client gets candidate 1
(`33 02 00`) and stops at verify — which is exactly what conn-3 above shows happening.

---

## 17. Session record — everything changed on 2026-09-03

Complete list of what this session touched, so §10-§16's findings can be traced back to code.
Changes the working tree already carried before this session (`CLIENT_PROTOCOL = 7.1.1`, the
`AnsVerify.createPacket(message)` argument fix, the `launcher/` package move) are **not** listed —
they were pre-existing and are unmodified except where noted.

### 17.1 auth-server code

| file | change | why |
|---|---|---|
| `server/NioServer.java` | `UniqueIpFilter` now gated on `AuthServerConfig.isUniqueIpFilter()` | the flag was read from the ini and never applied; the filter was unconditional (§10.1) |
| `server/handler/ClientPacketHandler.java` | `[conn-N]` tag on every line; per-connection id, duration and packet count; connects and zero-packet closes moved to DEBUG; first inbound packet announced at INFO; every packet logged as `<- #k <hex>`; close reports `having sent K packet(s)` | the launcher's 5-second heartbeat made real client traffic unreadable (§10.2) |
| ″ | probe path writes every packet in `attempt.packets()`, one `writeAndFlush` each | opcode bursts send eight replies; separate flushes keep them separate TCP segments under `TCP_NODELAY` (§13) |
| ″ | sends `AnsVerifyProbe.retriggerPacket()` after re-triggering candidates, bounded by `MAX_PROBE_RETRIGGERS = 40` | lets one client launch walk a whole candidate list (§13.1, §14.4) |
| ″ | the two inline `isAuthenticated` checks replaced by `isAllowed(...)`, which serves anyway while `ANS_VERIFY_PROBE` is on | `0x0E` makes the client skip `askAuthUser`, so the gate rejected exactly the traffic the probe produces (§15.3) |
| `server/handler/ServerPacketHandler.java` | stores the decoded `ServerInfo` into `GameServerRegistry` | it was decoded, logged and dropped; `AnsSrvList` needs it (§15.1) |
| `network/session/server/GameServerRegistry.java` | **new** — holds the `ServerInfo` from the S2S handshake | ″ |
| `network/packet/client/AnsSrvList.java` | rewritten to the §11.4 layouts, live server data, one synthesised channel | was `33 11 00` / `33 12 00` placeholders (§15.1, §16) |
| `network/packet/client/AnsVerify.java` | documented the layout; parameter renamed `payload` → `message` since it is the whole packet | the name actively misled - `VerifyUser` indexes from the packet start |
| `network/packet/client/handler/VerifyUser.java` | rewritten: offset-based field parsing, dotted-protocol comparison, US-ASCII address decode, **explicit logging of every rejection reason** | the old hex-string mangling (`replaceAll("0","")`) breaks on any version component of 0 or >= 10, and a silent `false` is indistinguishable from the client ignoring the reply (§3.1) |
| `network/packet/client/AnsVerifyProbe.java` | candidates can vary the opcode, not just the body; `Attempt` carries a packet **list** plus a re-trigger flag; added opcode bursts (11-14), one-opcode-per-launch sweep (15-46) and the `0x0E` driver (47) | §9.5's sweep, then §13.1's re-trigger, then §15.4's clean driver |

### 17.2 Configuration

- `Config/AuthServer/AuthServer.ini`
  - `UNIQUE_IP_FILTER = FALSE`, with a comment explaining the launcher/client collision (§10.1).
  - `[PROBE]` block rewritten: candidate map, re-trigger behaviour, the auth-gate relaxation, and
    how to read results.
  - `ANS_VERIFY_PROBE = TRUE` — **diagnostic state, not production.** Set `FALSE` to restore the
    normal `33 02 00` reply, at the cost of the client stopping at verify again.
- `Config/AuthServer/AnsVerifyProbe.state` — cursor `46` (candidate 47). Gitignored. **It wraps
  after every launch**, so reset it to `46` before each run (§16.4).

### 17.3 Tooling preserved outside the repo

`Classic\Development\Tools\Protocol Probes\` — two synthetic-client scripts, following §8's
convention of keeping session tooling out of git. Both talk raw TCP to `127.0.0.1:44405` and print
every frame as hex plus ASCII, so a layout can be checked **without a ~2-minute client launch and a
UAC prompt**:

- `Probe-AnsVerify.ps1` — connects, reads `A2U_ansReady`, sends a synthetic `U2A_askVerify`, prints
  whatever comes back. This is how the opcode bursts were verified before spending a launch.
- `Probe-SrvList.ps1` — the same, then sends `33 0F` and `33 13 00 00`, so `AnsSrvList` and
  `AnsSrvSelect` can be inspected byte-for-byte. This is how §15.1's 38/39-byte packets were
  confirmed before §16 put them in front of the real client.

Worth using for the server/channel work: with the probe on, `Probe-SrvList.ps1` exercises the whole
list path end to end in about a second.

### 17.4 Deliberately not changed

- **`AnsSrvSelect`** is still the 3-byte placeholder `33 1A 00` (§16.3).
- **database-server's `UniqueIpFilter`** is still unconditional and has no ini flag. Harmless with
  one S2S client; auth-server + game-server + web-server on `127.0.0.1` will collide (§10.1).
- **The commented-out `if/else` block** at the foot of `ClientPacketHandler` — dead, superseded by
  the `switch`, left alone.

### 17.5 Documentation

`CLIENT-PROTOCOL-NOTES.md`: §10-§17 added. Corrections applied in place, each pointing at what
superseded it — §3.1 and §9.6 by §11, §10.1 by §12.1, §3.3 obsoleted by §13.1, §9.5 disproved by
§14.1.

---

## 18. Handoff: the server/channel data layer

For the stored-procedure and database-server work that follows §16. Everything below was read off
the live `sun-classic` database on 2026-09-03.

### 18.1 The schema already exists, and it is empty

| table | rows | purpose |
|---|---|---|
| `GameServerInfo` | **0** | player-facing worlds - what `A2U_ansSrvList_Srv` should list |
| `ChannelInfo` | **0** | channels within a world - what `A2U_ansSrvList_Chn` should list |
| `ServerInfo` | 4 | **infrastructure registry, not worlds** - see §18.2 |

```
GameServerInfo   GameServerID int      Name nvarchar(15)   IpAddress varchar(20)
                 Port smallint         IsPvE char(5)       IsPvP char(5)
                 CreateDate datetime2  ModifiedDate datetime2 NULL

ChannelInfo      ChannelID int         GameServerID int    Name nvarchar(25)
                 IpAddress varchar(25) Port smallint
                 CreateDate datetime2  ModifiedDate datetime2 NULL
```

Existing procedures are `GetServerInfo` / `AddServerInfo` / `UpdateServerInfo` — all against
`ServerInfo`. **There is nothing for `GameServerInfo` or `ChannelInfo`.**

### 18.2 `ServerInfo` is the wrong table for the client — and `AnsSrvList` currently uses it

`ServerInfo` holds this stack's own servers and their **S2S** addresses:

```
1 | DATABASE SERVER | 127.0.0.1:10000
2 | AUTH SERVER     | 127.0.0.1:10001
7 | GAME SERVER     | 127.0.0.1:10002
8 | WEB SERVER      | 127.0.0.1:10003
```

§15 wired `AnsSrvList` to it through `GameServerRegistry` because it was the only live data
auth-server had, and §16 proved the *packet layouts* with it — the client happily displayed
`"GAME SERVER"` and selected it. **But it is the wrong source**, and the distinction matters most
at the next step: `10002` is the game server's S2S listener, not a client-facing port. Handing that
to the client in `A2U_ansSrvSelect` would point it at the wrong socket.

**So `AnsSrvList` should be repointed at `GameServerInfo` + `ChannelInfo` once they can be
queried.** `GameServerRegistry`'s class comment already says it is not a general registry and marks
where the real query belongs.

### 18.3 The address belongs to the channel — the schema already decided this

`ChannelInfo` carries its own `IpAddress` and `Port`. That answers the open design question for
`A2U_ansSrvSelect` (§16.3): the client sends `(Server#, Channel#)` and the reply carries one
`ConnectionIP` + `ConnectionPort`, so the address is looked up **per channel**, not per world.
`GameServerInfo.IpAddress`/`Port` is then the world's own address, presumably for the servers
themselves rather than for clients.

### 18.4 Wire constraints the data has to satisfy

From the layouts confirmed in §16.1, so the DDL and the procedures do not produce something
unsendable:

| wire field | width | schema | headroom |
|---|---|---|---|
| server name | 32 bytes, null-padded | `nvarchar(15)` | fine |
| channel name | **33** bytes, null-padded | `nvarchar(25)` | fine |
| server count | 1 byte | — | **max 255 worlds** |
| channel count | 1 byte | — | **max 255 channels per list** |
| `Server#` / `Channel#` | 1 byte each, **zero-based** | — | these are list indexes, not database ids |
| `ConnectionIP` (ansSrvSelect) | 32 bytes | `varchar(25)` | fine |
| `ConnectionPort` | 2 bytes | `smallint` | signed in SQL; ports above 32767 will need care |

Two things to be careful of: names are written as **US-ASCII** by `AnsSrvList#writeFixed` (it logs
and truncates anything that does not fit), so `nvarchar` content outside ASCII will not survive.
And `Server#`/`Channel#` are **positions in the list sent to the client**, not `GameServerID` /
`ChannelID` — whatever fetches the rows has to keep a stable order and map the client's echo back
to real ids.

### 18.5 What has to be built

1. **Stored procedures** — a `GetGameServerInfo` and a `GetChannelInfo`, following `GetServerInfo`'s
   shape (it takes `isAllServers` + `serverName`; the channel one wants a `GameServerID` filter).
2. **Entities.** `ChannelInfo` **already exists** at
   `database-server/.../database/entity/server/ChannelInfo.java` — mapped, but referenced by nothing:
   no DAO, no `@NamedStoredProcedureQuery`, no caller. There is **no `GameServerInfo` entity** at
   all. Two traps in the existing one:
   - `@NotBlank` is on `gameServerID` (int), `port` (short) and `createDate` (LocalDateTime).
     `@NotBlank` only applies to `CharSequence`; Jakarta Validation throws
     `UnexpectedTypeException` on those. Latent only because nothing validates the entity today.
   - It does not `implement Serializable`, unlike `ServerInfo`.
3. **DAOs** — alongside `ServerInfoDAO`, which already has an unused `getServerInfo()` returning
   every row.
4. **An S2S packet pair.** `Protocol.java`'s S2S block runs `0x01`..`0x06`, so **`0x07` and `0x08`
   are free** for `S2S_askServerList` / `S2S_ansServerList`. The existing
   `AskServerInfo`/`AnsServerInfo` pair is single-row and keyed by a category byte
   (`PacketHandler`'s `S2S_askServerInfo` branch maps `Category.AUTH` -> `"GAME SERVER"`), so it
   cannot be reused as-is. `AnsServerInfo` is the model to copy for encoding: explicit
   field-by-field `DataOutputStream`, no Java serialization. A list packet needs a count prefix and
   the same per-row encoding.
5. **Repoint `AnsSrvList`** at the new query, and drop the synthesised `"Channel 1"` placeholder.

### 18.6 Testing without a client launch

`Tools\Protocol Probes\Probe-SrvList.ps1` (§17.3) drives the whole path — `askVerify`,
`askSrvList`, `askSrvSelect` — over raw TCP in about a second, printing every frame as hex and
ASCII. With `ANS_VERIFY_PROBE = TRUE` it exercises the list end to end with no client, no UAC
prompt and no ~2-minute wait. Use it for the iteration, and save real client launches for
confirming the result.

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

**This is cheaper to test than the remaining body candidates:** sweep the opcode with a fixed
body instead of sweeping the body with a fixed opcode. Same one-launch-per-candidate cost.

### 9.6 The old `Ashime/LoginServer` repo — what it does and does not settle

Checked against <https://github.com/Ashime/LoginServer> (the project our packet classes were
ported from). It cannot supply the Classic `ansVerify` format, but it settles several things.

**Its `ansVerify` is byte-identical to ours — it is the *source* of our implementation, not
independent evidence.** `AnsVerifyPacket.createPacket` → `MessageEncoder.createShortPacket(0x33,
0x02, result)` → `Convert.intToByteArray` (2 bytes, big-endian) → `Utility.flip(0,1)`. Wire:
`03 00 | 33 02 00`. That is exactly the `0300330200` our `ClientPacketEncoder` logs today.

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

package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Serves a <b>different</b> {@code A2U_ansVerify} candidate on each connection, so candidates for
 * the still-unknown response format can be walked without a rebuild between each one. Candidates
 * come from {@code CLIENT-PROTOCOL-NOTES.md} §3, §9.5 and §13.
 *
 * <p><b>Opcode-sweep candidates chain within one launch; body candidates do not.</b> Each sweep
 * candidate is followed by a {@link #RETRIGGER} ({@code 33 00 00}, i.e. {@code A2U_ansReady}),
 * which makes the client send another {@code U2A_askVerify} on the same connection and so pull the
 * next candidate - discovered on 2026-09-03 (§13.1). The old "one candidate per client launch"
 * rule in §3.3 came from before that was known and is obsolete; it still applies to candidates
 * 1-10, which do not re-trigger so that new results stay comparable with the recorded ones.
 *
 * <p>The chain is bounded by {@code ClientPacketHandler}'s per-connection re-trigger budget, not
 * here - a client that re-verifies on every re-trigger would otherwise loop forever and wrap the
 * rotation.
 *
 * <p>Enabled by {@code [PROBE] ANS_VERIFY_PROBE} in {@code AuthServer.ini}; when off,
 * {@link AnsVerify} answers normally. This is a diagnostic, not production behaviour - it
 * deliberately sends replies known to be wrong.
 *
 * <h2>The rotation</h2>
 * <table>
 *   <caption>Candidate index ranges</caption>
 *   <tr><th>candidates</th><th>what</th><th>status</th></tr>
 *   <tr><td>1-3</td><td>body controls: {@code 00}, host mirror, empty</td><td>all failed (§3.4)</td></tr>
 *   <tr><td>4-10</td><td>further body shapes at opcode {@code 0x02}</td><td>4-8 failed; 9-10 untested</td></tr>
 *   <tr><td>11-14</td><td><b>opcode bursts</b> - see below</td><td>the current sweep</td></tr>
 *   <tr><td>15-46</td><td>one opcode per launch, {@code 0x00}..{@code 0x1F}, body {@code 00}</td><td>fallback</td></tr>
 * </table>
 *
 * <h2>Why the sweep bursts instead of trying one opcode per launch</h2>
 * §9.5 argues the reply <em>opcode</em> is the likelier variable than the body: seven structurally
 * different bodies at {@code 0x02} all failed identically, which is what an unrecognised opcode
 * looks like (dropped before the body is examined), and {@code A2U_ansVerify = 0x02} comes from the
 * old EP1 server rather than from any Classic capture. The EP1 documentation agrees on {@code 0x02}
 * (§11.1), but that is EP1's value, and this protocol's request/answer opcodes are <b>not</b>
 * regularly paired - {@code askAuthUser 0x03} answers on {@code 0x0E}, {@code askSrvList 0x0F} on
 * {@code 0x11}/{@code 0x12}, {@code askSrvSelect 0x13} on {@code 0x1A}. So the answer could sit
 * anywhere in the low opcode space.
 *
 * <p>Candidates 11-14 write <b>eight replies back to back on the one connection</b>, one per opcode,
 * so the whole space could be covered in four launches back when each launch was one candidate.
 * Candidate 12 ({@code 0x08}..{@code 0x0F}) is the one that hit (§13).
 *
 * <p><b>Bursts confound as much as they cover.</b> Burst 12 contains {@code 0x0E}, which is
 * {@code A2U_ansAuthUser} - a documented "authentication succeeded" - so the progression it
 * produced cannot be attributed to a verify reply without isolating. That is what candidates 15-46
 * are for: one opcode each, so a hit names its own opcode. Candidate number is
 * {@code 15 + opcode}, i.e. {@code 0x08} is candidate 23 and {@code 0x0F} is candidate 30.
 *
 * <p><b>What a hit looks like:</b> an inbound packet that is not another {@code U2A_askVerify} -
 * {@code 33 0F} ({@code U2A_askSrvList}) is what the client sent when it got past verify. The
 * {@code PROBE serving} line immediately before it names the candidate responsible.
 *
 * <p>Read results by pairing each {@code PROBE serving ...} line with the {@code PROBE result: ...}
 * line for the same connection (logged by {@code ClientPacketHandler#channelInactive}).
 * <b>Judge on whether the client sends any inbound byte, or reaches {@code U2A_askAuthUser} - not
 * on the close timing.</b> Timing is not reproducible: the same three bytes have produced 3.8s,
 * 14.8s, 5.2s and 1.4s closes across runs, so it cannot rank candidates (see §3.2 and §12.2). The
 * delay is logged only as weak context.
 *
 * <p><b>The rotation cursor survives restarts.</b> It is stored in {@link #STATE_FILE}, so stopping
 * auth-server between client launches - which happens constantly, since Windows locks a running jar
 * and every rebuild needs it stopped - no longer replays candidates already known to have failed.
 * Delete that file to restart the rotation from {@link #FIRST_UNTESTED_INDEX}, or write a
 * zero-based index into it to jump straight to a specific candidate.
 */
@Log4j2
public final class AnsVerifyProbe
{
    /** {@code U2A_askVerify} is {@code 07 01 01} then a 32-byte null-padded host field. */
    private static final int HOST_OFFSET = 3;
    private static final int HOST_LENGTH = 32;

    /** Inclusive opcode range the sweep covers, both as bursts and as one-per-launch fallbacks. */
    private static final int SWEEP_FIRST_OPCODE = 0x00;
    private static final int SWEEP_LAST_OPCODE = 0x1F;

    /** Opcodes per burst candidate. Four bursts cover the range above. */
    private static final int BURST_SIZE = 8;

    /** The body every opcode-sweep candidate carries - the documented {@code ansVerify} accept. */
    private static final byte[] SWEEP_BODY = { 0x00 };

    /**
     * Written after an opcode-sweep candidate to make the client send another
     * {@code U2A_askVerify} on the same connection, which pulls the next candidate.
     *
     * <p>Opcode {@code 0x00} is {@code A2U_ansReady}; re-sending it restarts the handshake.
     * Discovered by accident on 2026-09-03 (§13.1) when the {@code 0x00}-{@code 0x07} burst
     * contained it and the client re-verified immediately - which killed §3.3's "one candidate per
     * client launch" and lets the whole rotation run inside a single launch.
     *
     * <p>Deliberately the exact three bytes observed to work, <b>not</b> a well-formed
     * {@code ansReady} ({@code 33 00} plus a 4-byte TEA key). The short form is what the client
     * accepted, and a fresh TEA key here would also invalidate the one the session already handed
     * out.
     */
    private static final byte[] RETRIGGER = { Category.AUTH, Protocol.A2U_ansReady, 0x00 };

    /**
     * Where the rotation cursor is kept between runs, as a single zero-based index in plain text.
     * Sits beside the ini that turns the probe on, so both halves of the probe's state are found in
     * the same place. This is runtime state rather than configuration, so it is gitignored.
     */
    private static final Path STATE_FILE = Path.of("Config/AuthServer/AnsVerifyProbe.state");

    /**
     * Where a fresh rotation begins - zero-based, so index 3 is candidate 4 ({@code 0000}).
     *
     * <p>Candidates 1-3 ({@code 00} baseline, {@code 070101}+host mirror, and empty body) were each
     * served to a live client on 2026-09-01 and every one was closed on without a single inbound
     * byte (§3.4). Starting a fresh rotation at candidate 1 would spend three client launches
     * re-confirming that, so it starts past them instead. Set this to 0 to re-run the controls.
     */
    private static final int FIRST_UNTESTED_INDEX = 3;

    /**
     * One candidate: a label for the log, the reply packet(s) it writes, and whether a
     * {@link #RETRIGGER} should follow so the client asks again and pulls the next candidate.
     *
     * <p>Only the opcode-sweep candidates re-trigger. The body candidates (1-10) do not: they were
     * measured one per launch, and quietly changing how they are delivered would make new results
     * incomparable with the recorded ones.
     */
    private record Candidate(String label, Function<byte[], List<byte[]>> packets, boolean retrigger) {}

    /**
     * Ordered candidates. The cursor wraps, so the list can grow freely - it is walked one entry
     * per client launch either way.
     *
     * <p><b>Append new candidates rather than inserting them:</b> {@link #STATE_FILE} holds a
     * positional index, so reordering silently repoints a saved cursor at a different candidate.
     */
    private static final List<Candidate> CANDIDATES = buildCandidates();

    /** Always holds a valid index into {@link #CANDIDATES} - see {@link #readCursor()}. */
    private static final AtomicInteger cursor = new AtomicInteger(readCursor());

    private AnsVerifyProbe()
    {
    }

    /**
     * One served candidate: {@code label} for the log, {@code packets} for the wire, and whether
     * the caller should follow them with {@link #retriggerPacket()}.
     *
     * <p>The re-trigger is left to the caller rather than being folded into {@code packets} because
     * it needs a per-connection budget - see {@code ClientPacketHandler}. Nothing stops a client
     * that re-verifies on every re-trigger from looping, and the probe has no per-connection state
     * to bound it with.
     */
    public record Attempt(String label, List<byte[]> packets, boolean retrigger)
    {
        /** Every packet this candidate writes, space-separated, so one log line shows the lot. */
        public String hex()
        {
            return packets.stream().map(Utility::byteArrayToHexString).collect(Collectors.joining(" "));
        }
    }

    /** See {@link #RETRIGGER}. Copied so a caller cannot mutate the shared constant. */
    public static byte[] retriggerPacket()
    {
        return RETRIGGER.clone();
    }

    /**
     * Builds the next candidate in the rotation. Advances once per call, so each connection that
     * reaches {@code U2A_askVerify} gets the following entry, and persists the new position so a
     * restart resumes where this left off rather than replaying the rotation.
     *
     * @param askVerifyPayload the request body (after category+protocol), used by candidates that
     *                         echo the client's host field back
     */
    public static Attempt next(byte[] askVerifyPayload)
    {
        int index = cursor.getAndUpdate(current -> Math.floorMod(current + 1, CANDIDATES.size()));
        Candidate candidate = CANDIDATES.get(index);

        writeCursor(Math.floorMod(index + 1, CANDIDATES.size()));

        return new Attempt("[" + (index + 1) + "/" + CANDIDATES.size() + "] " + candidate.label(),
                candidate.packets().apply(askVerifyPayload), candidate.retrigger());
    }

    private static List<Candidate> buildCandidates()
    {
        List<Candidate> candidates = new ArrayList<>();

        // 1-3: the measured controls, kept first so the harness can be confirmed against §3's
        // numbers before any new row is trusted. FIRST_UNTESTED_INDEX skips past them.
        candidates.add(single("00 (baseline, 3-byte)", Protocol.A2U_ansVerify, payload -> new byte[] { 0x00 }));
        candidates.add(single("070101+host (mirror)", Protocol.A2U_ansVerify, AnsVerifyProbe::mirror));
        candidates.add(single("empty body", Protocol.A2U_ansVerify, payload -> new byte[0]));

        // 4-10: further body shapes, all still at opcode 0x02. 4-8 have been served and failed.
        candidates.add(single("0000", Protocol.A2U_ansVerify, payload -> new byte[] { 0x00, 0x00 }));
        candidates.add(single("00000000", Protocol.A2U_ansVerify, payload -> new byte[4]));
        candidates.add(single("0000000000000000", Protocol.A2U_ansVerify, payload -> new byte[8]));
        candidates.add(single("01", Protocol.A2U_ansVerify, payload -> new byte[] { 0x01 }));
        candidates.add(single("070101", Protocol.A2U_ansVerify, payload -> new byte[] { 0x07, 0x01, 0x01 }));
        candidates.add(single("00+host", Protocol.A2U_ansVerify, payload -> prepend((byte) 0x00, hostField(payload))));
        candidates.add(single("0001+host", Protocol.A2U_ansVerify, payload -> concat(new byte[] { 0x00, 0x01 }, hostField(payload))));

        // 11-14: the opcode sweep (§9.5), eight opcodes per launch. See the class comment.
        for(int first = SWEEP_FIRST_OPCODE; first <= SWEEP_LAST_OPCODE; first += BURST_SIZE)
            candidates.add(burst(first, Math.min(first + BURST_SIZE - 1, SWEEP_LAST_OPCODE)));

        // 15-46: the same opcodes one per launch, for bisecting a burst that hits - or for
        // re-running the sweep unpipelined if bursting itself proves to be the problem.
        for(int opcode = SWEEP_FIRST_OPCODE; opcode <= SWEEP_LAST_OPCODE; opcode++)
            candidates.add(sweepSingle(opcode));

        // 47: not a sweep entry - a driver. 0x0E is the one opcode the client acts on (§14.2), and
        // it takes the client to U2A_askSrvList. Deliberately does NOT re-trigger: the sweep
        // candidates would otherwise keep pulling the client back to askVerify while it is trying
        // to work through the server list, which makes the log unreadable at exactly the point
        // AnsSrvList is being tested. Point the cursor at index 46 to use it.
        candidates.add(new Candidate("0x0E only, no re-trigger (drive to askSrvList)",
                payload -> List.of(packet(Protocol.A2U_ansAuthUser, SWEEP_BODY)), false));

        return List.copyOf(candidates);
    }

    /** A candidate that writes one packet and does not re-trigger - the body candidates, 1-10. */
    private static Candidate single(String label, byte opcode, UnaryOperator<byte[]> body)
    {
        return new Candidate(label, payload -> List.of(packet(opcode, body.apply(payload))), false);
    }

    /** A one-opcode sweep candidate, followed by a re-trigger so the next one can be served. */
    private static Candidate sweepSingle(int opcode)
    {
        return new Candidate(String.format("opcode 0x%02X, body 00", opcode),
                payload -> List.of(packet((byte) opcode, SWEEP_BODY)), true);
    }

    /**
     * A candidate that writes one {@code body 00} reply per opcode in {@code [first, last]}, back
     * to back on the same connection - so one client launch covers eight opcodes instead of one.
     */
    private static Candidate burst(int first, int last)
    {
        String label = String.format("opcode burst 0x%02X-0x%02X, body 00 (%d packets)", first, last, last - first + 1);

        return new Candidate(label, payload ->
        {
            List<byte[]> packets = new ArrayList<>();

            for(int opcode = first; opcode <= last; opcode++)
                packets.add(packet((byte) opcode, SWEEP_BODY));

            return List.copyOf(packets);
        }, true);
    }

    /** Category + opcode + body. The 2-byte length header is added by {@code ClientPacketEncoder}. */
    private static byte[] packet(byte opcode, byte[] body)
    {
        byte[] packet = new byte[2 + body.length];
        packet[0] = Category.AUTH;
        packet[1] = opcode;
        System.arraycopy(body, 0, packet, 2, body.length);

        return packet;
    }

    /**
     * The saved cursor, or {@link #FIRST_UNTESTED_INDEX} when there is nothing usable to read.
     * A missing, empty, unparseable or unreadable file all mean the same thing here - no position
     * worth resuming - and none of them should stop auth-server starting over a diagnostic.
     */
    private static int readCursor()
    {
        try
        {
            if(Files.exists(STATE_FILE))
            {
                String saved = Files.readString(STATE_FILE).trim();

                if(!saved.isEmpty())
                {
                    // Wrapped rather than rejected, so that shrinking CANDIDATES cannot leave a
                    // saved index pointing past the end of the list.
                    int index = Math.floorMod(Integer.parseInt(saved), CANDIDATES.size());
                    log.info("Resuming ansVerify probe at candidate [{}/{}] ({}) from {}",
                            index + 1, CANDIDATES.size(), CANDIDATES.get(index).label(), STATE_FILE);

                    return index;
                }
            }
        }
        catch(IOException | NumberFormatException e)
        {
            log.warn("Could not read the ansVerify probe cursor from {} ({}) - starting at candidate [{}/{}].",
                    STATE_FILE, e.getMessage(), FIRST_UNTESTED_INDEX + 1, CANDIDATES.size());

            return FIRST_UNTESTED_INDEX;
        }

        log.info("No saved ansVerify probe cursor - starting at candidate [{}/{}].", FIRST_UNTESTED_INDEX + 1, CANDIDATES.size());

        return FIRST_UNTESTED_INDEX;
    }

    /**
     * Records where the rotation should resume. A failed write is logged and swallowed: losing the
     * position costs one repeated client launch, which is not worth failing a client's verify over.
     */
    private static void writeCursor(int nextIndex)
    {
        try
        {
            Path parent = STATE_FILE.getParent();

            if(parent != null)
                Files.createDirectories(parent);

            Files.writeString(STATE_FILE, Integer.toString(nextIndex));
        }
        catch(IOException e)
        {
            log.warn("Could not persist the ansVerify probe cursor to {} ({}) - a restart will replay from candidate [{}/{}].",
                    STATE_FILE, e.getMessage(), FIRST_UNTESTED_INDEX + 1, CANDIDATES.size());
        }
    }

    /** The client's 32-byte host field, or empty if the request was shorter than expected. */
    private static byte[] hostField(byte[] payload)
    {
        if(payload.length < HOST_OFFSET + HOST_LENGTH)
            return new byte[0];

        return Arrays.copyOfRange(payload, HOST_OFFSET, HOST_OFFSET + HOST_LENGTH);
    }

    /** Echoes the request back verbatim - the 1.9s "actively rejected" row in §3. */
    private static byte[] mirror(byte[] payload)
    {
        return payload.clone();
    }

    private static byte[] prepend(byte first, byte[] rest)
    {
        return concat(new byte[] { first }, rest);
    }

    private static byte[] concat(byte[] head, byte[] tail)
    {
        byte[] out = new byte[head.length + tail.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }
}

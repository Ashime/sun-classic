package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

/**
 * Serves a <b>different</b> {@code A2U_ansVerify} body on each connection, so candidates for the
 * still-unknown response format can be walked without a rebuild between each one. Candidates come
 * from {@code CLIENT-PROTOCOL-NOTES.md} §3.
 *
 * <p><b>One candidate per client launch.</b> {@code SERVICE_LOGIN_TRY_COUNTS = 10} in the client's
 * {@code LOGIN.INI} looks like it should let a single launch walk the whole list, and this class
 * was originally built on that assumption - but it is wrong. Observed across three launches, the
 * client makes exactly one {@code U2A_askVerify} attempt, then exits; the retry budget appears to
 * cover failures to *connect*, not a connection that succeeds and gets an unsatisfactory reply
 * (see §3.3). Budget one full client launch, roughly 45 seconds, per candidate.
 *
 * <p>Enabled by {@code [PROBE] ANS_VERIFY_PROBE} in {@code AuthServer.ini}; when off,
 * {@link AnsVerify} answers normally. This is a diagnostic, not production behaviour - it
 * deliberately sends replies known to be wrong.
 *
 * <p>Read results by pairing each {@code PROBE serving ...} line with the {@code PROBE result: ...}
 * line for the same connection (logged by {@code ClientPacketHandler#channelInactive}).
 * <b>Judge on whether the client sends any inbound byte, or reaches {@code U2A_askAuthUser} - not
 * on the close timing.</b> Timing is not reproducible: the same three bytes have produced 3.8s,
 * 14.8s and 5.2s closes across runs, so it cannot rank candidates (see §3.2). The delay is logged
 * only as weak context.
 *
 * <p><b>The rotation cursor survives restarts.</b> It is stored in {@link #STATE_FILE}, so stopping
 * auth-server between client launches - which happens constantly, since Windows locks a running jar
 * and every rebuild needs it stopped - no longer replays candidates already known to have failed.
 * A launch costs roughly 45 seconds, so replaying even one is worth avoiding. Delete that file to
 * restart the rotation from {@link #FIRST_UNTESTED_INDEX}, or write a zero-based index into it to
 * jump straight to a specific candidate.
 */
@Log4j2
public final class AnsVerifyProbe
{
    /** {@code U2A_askVerify} is {@code 07 01 01} then a 32-byte null-padded host field. */
    private static final int HOST_OFFSET = 3;
    private static final int HOST_LENGTH = 32;

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

    private record Candidate(String label, UnaryOperator<byte[]> body) {}

    /**
     * Ordered candidate bodies (everything after category+protocol). The cursor wraps, so the list
     * can grow freely - it is walked one entry per client launch either way.
     *
     * <p>Append new candidates rather than inserting them: {@link #STATE_FILE} holds a positional
     * index, so reordering this list silently repoints a saved cursor at a different candidate.
     */
    private static final List<Candidate> CANDIDATES = List.of(
            // The three already measured, kept first as controls to confirm the harness reproduces
            // §3's numbers before trusting any new row. FIRST_UNTESTED_INDEX skips past them.
            new Candidate("00 (baseline, 3-byte)", payload -> new byte[] { 0x00 }),
            new Candidate("070101+host (mirror)", AnsVerifyProbe::mirror),
            new Candidate("empty body", payload -> new byte[0]),

            // Untested candidates from §3.
            new Candidate("0000", payload -> new byte[] { 0x00, 0x00 }),
            new Candidate("00000000", payload -> new byte[4]),
            new Candidate("0000000000000000", payload -> new byte[8]),
            new Candidate("01", payload -> new byte[] { 0x01 }),
            new Candidate("070101", payload -> new byte[] { 0x07, 0x01, 0x01 }),
            new Candidate("00+host", payload -> prepend((byte) 0x00, hostField(payload))),
            new Candidate("0001+host", payload -> concat(new byte[] { 0x00, 0x01 }, hostField(payload)))
    );

    /** Always holds a valid index into {@link #CANDIDATES} - see {@link #readCursor()}. */
    private static final AtomicInteger cursor = new AtomicInteger(readCursor());

    private AnsVerifyProbe()
    {
    }

    /** One served candidate: {@code label} for the log, {@code packet} for the wire. */
    public record Attempt(String label, byte[] packet)
    {
        public String hex()
        {
            return Utility.byteArrayToHexString(packet);
        }
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

        byte[] body = candidate.body().apply(askVerifyPayload);
        byte[] packet = new byte[2 + body.length];
        packet[0] = Category.AUTH;
        packet[1] = Protocol.A2U_ansVerify;
        System.arraycopy(body, 0, packet, 2, body.length);

        return new Attempt("[" + (index + 1) + "/" + CANDIDATES.size() + "] " + candidate.label(), packet);
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
                    log.info("Resuming ansVerify probe at candidate [{}/{}] from {}", index + 1, CANDIDATES.size(), STATE_FILE);

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

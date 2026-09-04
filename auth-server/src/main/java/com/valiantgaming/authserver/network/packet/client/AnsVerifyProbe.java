package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;

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
 * <p>The rotation cursor is static and resets when auth-server restarts, so a restart puts the
 * next launch back on candidate 1.
 */
public final class AnsVerifyProbe
{
    /** {@code U2A_askVerify} is {@code 07 01 01} then a 32-byte null-padded host field. */
    private static final int HOST_OFFSET = 3;
    private static final int HOST_LENGTH = 32;

    private record Candidate(String label, UnaryOperator<byte[]> body) {}

    /**
     * Ordered candidate bodies (everything after category+protocol). The cursor wraps, so the list
     * can grow freely - it is walked one entry per client launch either way.
     */
    private static final List<Candidate> CANDIDATES = List.of(
            // The two already measured, kept first as controls to confirm the harness reproduces
            // §3's numbers before trusting any new row.
            new Candidate("00 (baseline, 3-byte)", payload -> new byte[] { 0x00 }),
            new Candidate("070101+host (mirror)", AnsVerifyProbe::mirror),

            // Untested candidates from §3.
            new Candidate("empty body", payload -> new byte[0]),
            new Candidate("0000", payload -> new byte[] { 0x00, 0x00 }),
            new Candidate("00000000", payload -> new byte[4]),
            new Candidate("0000000000000000", payload -> new byte[8]),
            new Candidate("01", payload -> new byte[] { 0x01 }),
            new Candidate("070101", payload -> new byte[] { 0x07, 0x01, 0x01 }),
            new Candidate("00+host", payload -> prepend((byte) 0x00, hostField(payload))),
            new Candidate("0001+host", payload -> concat(new byte[] { 0x00, 0x01 }, hostField(payload)))
    );

    private static final AtomicInteger cursor = new AtomicInteger();

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
     * reaches {@code U2A_askVerify} gets the following entry.
     *
     * @param askVerifyPayload the request body (after category+protocol), used by candidates that
     *                         echo the client's host field back
     */
    public static Attempt next(byte[] askVerifyPayload)
    {
        int index = Math.floorMod(cursor.getAndIncrement(), CANDIDATES.size());
        Candidate candidate = CANDIDATES.get(index);

        byte[] body = candidate.body().apply(askVerifyPayload);
        byte[] packet = new byte[2 + body.length];
        packet[0] = Category.AUTH;
        packet[1] = Protocol.A2U_ansVerify;
        System.arraycopy(body, 0, packet, 2, body.length);

        return new Attempt("[" + (index + 1) + "/" + CANDIDATES.size() + "] " + candidate.label(), packet);
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

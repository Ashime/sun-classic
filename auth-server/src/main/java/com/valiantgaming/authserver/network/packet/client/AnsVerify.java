package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.authserver.network.packet.client.handler.VerifyUser;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2U_ansVerify}.
 *
 * <pre>
 * Size     2 bytes (little-endian, added by ClientPacketEncoder)
 * Category 1 byte  0x33
 * Protocol 1 byte  0x02
 * Flag     1 byte  0x00 = accepted, 0x01 = rejected
 * </pre>
 *
 * <p>That layout is what the EP1 {@code ServerPackets} documentation specifies, and it is what
 * this class has always emitted. It is <b>not</b> confirmed against the Classic 2.6.0.1 client -
 * see {@code CLIENT-PROTOCOL-NOTES.md} §3, where seven structurally different bodies were all
 * closed on without a single inbound byte. §10 records the reason those results are now suspect:
 * with {@code UNIQUE_IP_FILTER = TRUE} an open launcher could take the client's connection down
 * before the reply mattered, so a failure there did not necessarily mean the reply was wrong.
 *
 * <p>The flag polarity ({@code 0x00} accepted) is documented, matches {@code AnsAuthUser}, and
 * was separately shown not to be the blocker - probe candidate 7 sent {@code 0x01} as a success
 * and failed identically. It still matters that {@link VerifyUser#verify} only fails a genuinely
 * malformed request: {@code 0x01} is a hard refusal.
 */
public class AnsVerify
{
    /**
     * @param message the whole {@code U2A_askVerify} packet including its category and protocol
     *                bytes - {@link VerifyUser#verify} indexes from the start of the packet, not
     *                from the start of the payload
     */
    public static byte[] createPacket(byte[] message)
    {
        byte flag;

        if(VerifyUser.verify(message))
        {
            flag = 0x00;
        }
        else
        {
            // This is false to the client.
            flag = 0x01;
        }

        return new byte[] { Category.AUTH, Protocol.A2U_ansVerify, flag };
    }
}

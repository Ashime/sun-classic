package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.authserver.network.session.client.ClientSession;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

import java.security.SecureRandom;

/**
 * Builds {@code A2U_ansReady}, the first packet AuthServer sends a connected game client -
 * mirrors the launcher's {@code A2L_ansReady} in intent (hand out the TEA key that turns on
 * packet encryption for the rest of the session).
 *
 * <p>No example of this packet has been captured, so there's no confirmed key length or
 * exchange method (e.g. whether it's RSA-wrapped like the S2S AES key, or sent plain). This
 * generates a random 16-byte placeholder so the handshake shell has something to send/store;
 * revisit once real capture data exists. {@link com.valiantgaming.commons.security.crypt.TEA}
 * itself is also unimplemented, so nothing downstream actually uses this key yet.
 */
public class AnsReady
{
    private static final SecureRandom RANDOM = new SecureRandom();

    public byte[] createPacket(ClientSession session)
    {
        byte[] teaKey = new byte[16];
        RANDOM.nextBytes(teaKey);

        session.setTeaKey(teaKey);
        session.setMessageCryptEnabled(true);

        byte[] packet = new byte[2 + teaKey.length];
        packet[0] = Category.AUTH;
        packet[1] = Protocol.A2U_ansReady;
        System.arraycopy(teaKey, 0, packet, 2, teaKey.length);

        return packet;
    }
}

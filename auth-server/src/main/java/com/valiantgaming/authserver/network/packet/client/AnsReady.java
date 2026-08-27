package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.authserver.network.session.client.ClientSession;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.security.crypt.TEA;

/**
 * Builds {@code A2U_ansReady}, the first packet AuthServer sends a connected game client -
 * mirrors the launcher's {@code A2L_ansReady} in intent (hand out the TEA key used to
 * encrypt the password field in {@code U2A_askAuthUser}).
 *
 * <p>The key is 4 bytes - see {@link TEA}'s class comment for why (it's expanded into 4
 * single-byte round-key slots, not a real 128-bit key). No example of this packet's exact
 * framing has been captured (e.g. whether the key is sent plain like this, or wrapped
 * somehow), so that part is still a placeholder.
 */
public class AnsReady
{
    public byte[] createPacket(ClientSession session)
    {
        byte[] teaKey = TEA.generateKey();

        // Not session.setMessageCryptEnabled(true) here: TEA only decrypts the password field
        // in U2A_askAuthUser (see AuthUser), it doesn't cover whole packets, so there's
        // nothing for that flag to gate yet - see ClientPacketDecoder's class comment.
        session.setTeaKey(teaKey);

        byte[] packet = new byte[2 + teaKey.length];
        packet[0] = Category.AUTH;
        packet[1] = Protocol.A2U_ansReady;
        System.arraycopy(teaKey, 0, packet, 2, teaKey.length);

        return packet;
    }
}

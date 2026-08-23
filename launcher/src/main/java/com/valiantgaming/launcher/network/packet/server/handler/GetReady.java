package com.valiantgaming.launcher.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;

/**
 * Decodes {@code A2L_ansReady}, which per the flow comment in {@code Protocol} delivers the
 * TEA key that turns on packet encryption for the rest of the launcher/AuthServer handshake.
 *
 * <p>No example of this packet has been captured yet (unlike {@code A2L_ansVerifyVersion}),
 * so the key is taken as "everything after category+protocol" with no length validation.
 * Revisit once a real capture confirms the key size/format.
 */
public class GetReady
{
    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }
}

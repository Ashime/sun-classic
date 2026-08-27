package com.valiantgaming.launcher.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;

/**
 * Decodes {@code A2U_ansReady} - the packet AuthServer sends unconditionally to every new
 * connection (launcher or real game client alike, see auth-server's {@code ClientPacketHandler}
 * class comment) before either side has said anything else. Unlike {@code A2L_ansReady}
 * (see {@link GetReady}, which carries no payload), this one actually carries the 4-byte TEA
 * key used to encrypt {@code U2A_askAuthUser}'s password field - see {@code AnsReady} on the
 * server side.
 */
public class GetAuthReady
{
    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }
}

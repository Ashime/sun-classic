package com.valiantgaming.launcher.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;

/**
 * Decodes {@code A2L_ansReady}. The original flow notes in {@code Protocol} labelled this
 * packet "(Tea Key)", but {@link com.valiantgaming.commons.security.crypt.TEA} turned out to
 * only ever decrypt a password field, and the launcher never sends one - see
 * {@code AnsLauncherReady}'s class comment on the server side, which currently sends this
 * packet with no payload at all.
 *
 * <p>Still returns "everything after category+protocol" rather than assuming zero length,
 * in case a real capture of this packet later shows it does carry something.
 */
public class GetReady
{
    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }
}

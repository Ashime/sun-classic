package com.valiantgaming.authserver.network.packet.client.launcher;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2L_ansReady}, sent in reply to the launcher's {@code L2A_askUnknown1} (see
 * {@code ClientPacketHandler}'s launcher branch).
 *
 * <p>Unlike {@code AnsReady} (the game client's {@code A2U_ansReady}), this carries no
 * payload. The original flow notes labeled this packet "(Tea Key)", but {@link
 * com.valiantgaming.commons.security.crypt.TEA} turned out to only ever decrypt a password
 * field (see its class comment) - and the launcher never sends a password - so there's
 * nothing confirmed to hand out here. Revisit if a real capture of this packet shows
 * otherwise.
 */
public class AnsLauncherReady
{
    public byte[] createPacket()
    {
        return new byte[] { Category.AUTH, Protocol.A2L_ansReady };
    }
}

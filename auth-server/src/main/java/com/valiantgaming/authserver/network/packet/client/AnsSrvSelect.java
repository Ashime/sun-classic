package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2U_ansSrvSelect}, sent in response to {@code U2A_askSrvSelect}. This is
 * presumably where the client gets whatever connection info it needs to hand off to the
 * selected game-server, but no example of either packet has been captured - placeholder
 * acknowledgement only until the real payload is known.
 */
public class AnsSrvSelect
{
    public byte[] createPacket()
    {
        return new byte[] { Category.AUTH, Protocol.A2U_ansSrvSelect, 0x00 };
    }
}

package com.valiantgaming.launcher.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * First packet the launcher sends once connected to AuthServer. Per the captured flow in
 * {@link Protocol}, {@code L2A_askUnknown1} carries no payload - just category/opcode.
 */
public class AskUnknown1
{
    public byte[] createPacket()
    {
        return new byte[] { Category.AUTH, Protocol.L2A_askUnknown1 };
    }
}

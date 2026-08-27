package com.valiantgaming.webserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

public class AskServerInfo
{
    public byte[] createPacket()
    {
        return new byte[] { Category.DATABASE, Protocol.S2S_askServerInfo, Category.WEBSITE };
    }
}

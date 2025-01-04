package com.valiantgaming.authserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

import java.nio.charset.StandardCharsets;

public class AskAesFileKey
{
    public byte[] createPacket(String channelID)
    {
        byte[] id = channelID.getBytes(StandardCharsets.UTF_8);
        byte[] packet = new byte[id.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_askAesFileKey;
        System.arraycopy(id, 0, packet, 2, id.length);

        return packet;
    }
}
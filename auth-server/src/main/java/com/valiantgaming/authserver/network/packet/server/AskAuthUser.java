package com.valiantgaming.authserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class AskAuthUser
{
    @SneakyThrows
    public byte[] createPacket(int requestId, String username, String password)
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try(DataOutputStream data = new DataOutputStream(byteStream))
        {
            data.writeInt(requestId);
            data.writeUTF(username);
            data.writeUTF(password);
        }

        byte[] data = byteStream.toByteArray();
        byte[] packet = new byte[data.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_askAuthUser;
        System.arraycopy(data, 0, packet, 2, data.length);

        return packet;
    }
}

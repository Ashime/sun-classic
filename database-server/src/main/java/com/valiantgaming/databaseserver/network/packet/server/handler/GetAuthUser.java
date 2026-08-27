package com.valiantgaming.databaseserver.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class GetAuthUser
{
    public record AuthUserRequest(int requestId, String username, String password) {}

    @SneakyThrows
    public static AuthUserRequest decode(byte[] message)
    {
        byte[] payload = Utility.split(message, 2, message.length);

        try(DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload)))
        {
            int requestId = data.readInt();
            String username = data.readUTF();
            String password = data.readUTF();

            return new AuthUserRequest(requestId, username, password);
        }
    }
}

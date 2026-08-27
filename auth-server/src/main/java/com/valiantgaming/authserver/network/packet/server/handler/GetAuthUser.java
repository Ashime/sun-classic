package com.valiantgaming.authserver.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class GetAuthUser
{
    public record AuthUserResult(int requestId, boolean authenticated) {}

    @SneakyThrows
    public static AuthUserResult decode(byte[] message)
    {
        byte[] payload = Utility.split(message, 2, message.length);

        try(DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload)))
        {
            int requestId = data.readInt();
            boolean authenticated = data.readBoolean();

            return new AuthUserResult(requestId, authenticated);
        }
    }
}

package com.valiantgaming.webserver.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class GetCreateAccount
{
    public record CreateAccountResult(int requestId, String message) {}

    @SneakyThrows
    public static CreateAccountResult decode(byte[] message)
    {
        byte[] payload = Utility.split(message, 2, message.length);

        try(DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload)))
        {
            int requestId = data.readInt();
            String resultMessage = data.readUTF();

            return new CreateAccountResult(requestId, resultMessage);
        }
    }
}

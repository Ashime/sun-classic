package com.valiantgaming.webserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.webserver.web.RegisterAccountRequest;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class AskCreateAccount
{
    @SneakyThrows
    public byte[] createPacket(int requestId, RegisterAccountRequest request)
    {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try(DataOutputStream data = new DataOutputStream(byteStream))
        {
            data.writeInt(requestId);
            data.writeUTF(request.username());
            data.writeUTF(request.password());
            data.writeUTF(request.email());
            data.writeUTF(nullToEmpty(request.firstName()));
            data.writeUTF(request.lastName());
            data.writeInt(request.birthMonth());
            data.writeInt(request.birthDay());
            data.writeUTF(nullToEmpty(request.securityQuestion1()));
            data.writeUTF(nullToEmpty(request.answer1()));
            data.writeUTF(nullToEmpty(request.securityQuestion2()));
            data.writeUTF(nullToEmpty(request.answer2()));
            data.writeUTF(nullToEmpty(request.securityQuestion3()));
            data.writeUTF(nullToEmpty(request.answer3()));
        }

        byte[] data = byteStream.toByteArray();
        byte[] packet = new byte[data.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_askCreateAccount;
        System.arraycopy(data, 0, packet, 2, data.length);

        return packet;
    }

    // firstName and the security question/answer fields are optional on RegisterAccountRequest
    // (see the Profile table's Allow Nulls column), but DataOutputStream#writeUTF throws on null.
    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }
}

package com.valiantgaming.authserver.network.packet.server.handler;

import com.valiantgaming.authserver.database.entity.server.ServerInfo;
import com.valiantgaming.commons.utility.Utility;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.time.LocalDateTime;

public class GetServerInfo
{
    @SneakyThrows
    public static ServerInfo decode(byte[] message)
    {
        byte[] payload = Utility.split(message, 2, message.length);

        try(DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload)))
        {
            ServerInfo serverInfo = new ServerInfo();

            serverInfo.setServerID(data.readInt());
            serverInfo.setServerName(nullIfEmpty(data.readUTF()));
            serverInfo.setPublicIP(nullIfEmpty(data.readUTF()));
            serverInfo.setPublicPort(nullIfEmpty(data.readUTF()));
            serverInfo.setIpv4(nullIfEmpty(data.readUTF()));
            serverInfo.setIpv4Port(nullIfEmpty(data.readUTF()));
            serverInfo.setLocalIP(nullIfEmpty(data.readUTF()));
            serverInfo.setLocalPort(nullIfEmpty(data.readUTF()));
            serverInfo.setPublicEnabled(data.readBoolean());
            serverInfo.setIpv4Enabled(data.readBoolean());
            serverInfo.setLocalEnabled(data.readBoolean());

            String createDate = data.readUTF();
            if(!createDate.isEmpty())
            {
                serverInfo.setCreateDate(LocalDateTime.parse(createDate));
            }

            String modifiedDate = data.readUTF();
            if(!modifiedDate.isEmpty())
            {
                serverInfo.setModifiedDate(LocalDateTime.parse(modifiedDate));
            }

            return serverInfo;
        }
    }

    private static String nullIfEmpty(String value)
    {
        return value.isEmpty() ? null : value;
    }
}

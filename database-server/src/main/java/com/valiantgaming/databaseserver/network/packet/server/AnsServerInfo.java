package com.valiantgaming.databaseserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.databaseserver.database.DatabaseManager;
import com.valiantgaming.databaseserver.database.HibernateSession;
import com.valiantgaming.databaseserver.database.dao.server.ServerInfoDAO;
import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.Future;

public class AnsServerInfo
{
    @SneakyThrows
    public byte[] createPacket(String serverName)
    {
        ServerInfo serverInfo = null;
        Future<Object> futureTask = DatabaseManager.getInstance().submitTask(() -> new ServerInfoDAO(HibernateSession.createSession()).getServerByName(serverName));

        do
        {
            if(futureTask.get() != null)
            {
                serverInfo = (ServerInfo) futureTask.get();
            }

        } while(!futureTask.isDone());

        assert serverInfo != null;

        // Explicit field-by-field encoding instead of Java's ObjectOutputStream: DataOutputStream only writes
        // primitives/strings, so there's no class-metadata coupling to auth-server's own ServerInfo class and no
        // deserialization-gadget-chain risk on the receiving end.
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try(DataOutputStream data = new DataOutputStream(byteStream))
        {
            data.writeInt(serverInfo.getServerID());
            data.writeUTF(emptyIfNull(serverInfo.getServerName()));
            data.writeUTF(emptyIfNull(serverInfo.getPublicIP()));
            data.writeUTF(emptyIfNull(serverInfo.getPublicPort()));
            data.writeUTF(emptyIfNull(serverInfo.getIpv4()));
            data.writeUTF(emptyIfNull(serverInfo.getIpv4Port()));
            data.writeUTF(emptyIfNull(serverInfo.getLocalIP()));
            data.writeUTF(emptyIfNull(serverInfo.getLocalPort()));
            data.writeBoolean(serverInfo.isPublicEnabled());
            data.writeBoolean(serverInfo.isIpv4Enabled());
            data.writeBoolean(serverInfo.isLocalEnabled());
            data.writeUTF(serverInfo.getCreateDate() != null ? serverInfo.getCreateDate().toString() : "");
            data.writeUTF(serverInfo.getModifiedDate() != null ? serverInfo.getModifiedDate().toString() : "");
        }

        byte[] data = byteStream.toByteArray();
        byte[] packet = new byte[data.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansServerInfo;
        System.arraycopy(data, 0, packet, 2, data.length);

        return packet;
    }

    private static String emptyIfNull(String value)
    {
        return value == null ? "" : value;
    }
}
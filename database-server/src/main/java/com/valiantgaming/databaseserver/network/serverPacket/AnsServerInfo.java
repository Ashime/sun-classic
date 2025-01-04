package com.valiantgaming.databaseserver.network.serverPacket;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.databaseserver.database.DatabaseManager;
import com.valiantgaming.databaseserver.database.HibernateSession;
import com.valiantgaming.databaseserver.database.dao.server.ServerInfoDAO;
import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import lombok.SneakyThrows;
import org.springframework.util.SerializationUtils;

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
        byte[] data = SerializationUtils.serialize(serverInfo);

        assert data != null;
        byte[] packet = new byte[data.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansServerInfo;
        System.arraycopy(data, 0, packet, 3, data.length);

        return packet;
    }
}
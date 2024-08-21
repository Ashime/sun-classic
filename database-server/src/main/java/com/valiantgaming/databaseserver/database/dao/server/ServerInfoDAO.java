package com.valiantgaming.databaseserver.database.dao.server;

import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;

public interface ServerInfoDAO
{
    boolean add(ServerInfo serverInfo);

    int getServerID(String serverName);

    boolean update(ServerInfo serverInfo);
}
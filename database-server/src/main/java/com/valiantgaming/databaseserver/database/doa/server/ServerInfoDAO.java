package com.valiantgaming.databaseserver.database.doa.server;

import com.valiantgaming.databaseserver.database.entity.ServerInfo;

public interface ServerInfoDAO
{
    boolean add(ServerInfo serverInfo);

    int getServerID(String serverName);

    boolean update(ServerInfo serverInfo);
}
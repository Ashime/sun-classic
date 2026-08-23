package com.valiantgaming.databaseserver.database.dao.server;

import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;

import java.util.List;

@Log4j2
public class ServerInfoDAO
{
    private ServerInfo serverInfo;
    private final Session session;

    public ServerInfoDAO(Session session)
    {
        this.session = session;
    }

    public ServerInfo getServerByName(String serverName)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("GetServerInfo")
                .setParameter("isAllServers", false)
                .setParameter("serverName", serverName);

        if(query.execute())
        {
            return (ServerInfo) query.getSingleResult();
        }
        else
            log.error("SP GetServerInfo - Procedure failed and return value is NULL!");

        return null;
    }

    public List<ServerInfo> getServerInfo()
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("GetServerInfo")
                .setParameter("isAllServers", true)
                .setParameter("serverName", null);

        if(query.execute())
        {
            return (List<ServerInfo>) query.getResultList();
        }
        else
            log.error("SP GetServerInfo - Procedure failed and return value is NULL!");

        return null;
    }
}
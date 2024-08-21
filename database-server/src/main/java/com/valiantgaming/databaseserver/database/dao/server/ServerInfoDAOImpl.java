package com.valiantgaming.databaseserver.database.dao.server;

import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Getter
@Repository
public class ServerInfoDAOImpl implements ServerInfoDAO
{
    private final EntityManager entityManager;

    @Autowired
    public ServerInfoDAOImpl(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public boolean add(ServerInfo serverInfo)
    {
        StoredProcedureQuery query = entityManager.createNamedStoredProcedureQuery("AddServerInfo")
                .setParameter("serverInfo", serverInfo);

        boolean status = query.execute();

        if(status)
            if(query.getSingleResult().toString().equals("SUCCESS!"))
                return true;

        log.error("AddServerInfo - " + query.getSingleResult().toString());
        return false;
    }

    @Override
    public int getServerID(String serverName)
    {
        StoredProcedureQuery query = entityManager.createNamedStoredProcedureQuery("GetServerInfo")
                .setParameter("serverName", serverName);

        return 0;
    }

    @Override
    @Transactional
    public boolean update(ServerInfo serverInfo)
    {
        StoredProcedureQuery query = entityManager.createNamedStoredProcedureQuery("UpdateServerInfo")
                .setParameter("serverInfo", serverInfo);

        boolean status = query.execute();

        if(status)
            if(query.getSingleResult().toString().equals("SUCCESS!"))
                return true;

        log.error("UpdateServerInfo - " + query.getSingleResult().toString());
        return false;
    }
}
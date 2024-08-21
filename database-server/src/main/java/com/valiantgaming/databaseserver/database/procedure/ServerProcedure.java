package com.valiantgaming.databaseserver.database.procedure;

import com.valiantgaming.databaseserver.database.doa.server.ServerInfoDAO;
import com.valiantgaming.databaseserver.database.entity.ServerInfo;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

@Log4j2
public class ServerProcedure
{
    @NamedStoredProcedureQueries
    ({
        @NamedStoredProcedureQuery
        (
            name = "AddServerInfo",
            procedureName = "AddServerInfo",
            resultClasses = { String.class },
            parameters =
            {
                @StoredProcedureParameter
                (
                    name = "serverInfo",
                    type = ServerInfo.class,
                    mode = ParameterMode.IN
                )
            }
        ),
        @NamedStoredProcedureQuery
        (
            name = "GetServerID",
            procedureName = "GetServerID",
            resultClasses = { Integer.class },
            parameters =
            {
                @StoredProcedureParameter
                (
                    name = "serverName",
                    type = String.class,
                    mode = ParameterMode.IN
                )
            }
        ),
        @NamedStoredProcedureQuery
        (
            name = "UpdateServerInfo",
            procedureName = "UpdateServerInfo",
            resultClasses = { String.class },
            parameters =
            {
                @StoredProcedureParameter
                (
                    name = "serverInfo",
                    type = ServerInfo.class,
                    mode = ParameterMode.IN
                )
            }
        )
    })
    public static class ServerInfoProcedure
    {
        private final ServerInfoDAO serverInfoDAO;

        @Autowired
        public ServerInfoProcedure(ServerInfoDAO serverInfoDAO)
        {
            this.serverInfoDAO = serverInfoDAO;
        }

        public void addServerInfo(ServerInfo serverInfo)
        {
            serverInfoDAO.add(serverInfo);
        }

        public int getServerID(String serverName)
        {
            return serverInfoDAO.getServerID(serverName);
        }

        public void updateServerInfo(ServerInfo serverInfo)
        {
            serverInfoDAO.update(serverInfo);
        }
    }
}
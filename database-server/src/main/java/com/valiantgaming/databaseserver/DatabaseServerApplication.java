package com.valiantgaming.databaseserver;

import com.valiantgaming.commons.security.hash.SHA;
import com.valiantgaming.databaseserver.config.ConfigManager;
import com.valiantgaming.databaseserver.database.DatabaseManager;
import com.valiantgaming.databaseserver.database.HibernateSession;
import com.valiantgaming.databaseserver.database.dao.account.AccountDAO;
import com.valiantgaming.databaseserver.database.entity.Profile;
import com.valiantgaming.databaseserver.database.entity.account.Account;
import com.valiantgaming.databaseserver.server.NioServer;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j2
@SpringBootApplication
public class DatabaseServerApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(DatabaseServer.class, args);
    }

    public static class DatabaseServer
    {
        @SneakyThrows
        public DatabaseServer()
        {
            /*
                1. DatbaseServer - Post Spring
                    >> Config Manager
                        a. Database Manager Config
                        b. Database Server Config
                        c. Account Config
                        d. Character Config
                    >> Database Manager
                        a. Encryption Keys
                        b. Executor Service (Tasks)
                            1. Server Info (IpRules)
                            2. Character Deletion
                            3. Account Deletion
                    >> Nio-Server
             */
            ConfigManager.getInstance();
            DatabaseManager.getInstance();
            NioServer.getInstance();

        }
    }
}
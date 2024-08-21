package com.valiantgaming.databaseserver;

import com.valiantgaming.databaseserver.config.DatabaseServerConfig;
import com.valiantgaming.databaseserver.database.DatabaseManager;
import com.valiantgaming.databaseserver.network.server.NioServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabaseServerApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(DatabaseServer.class, args);
    }

    public static class DatabaseServer
    {
        public DatabaseServer()
        {
            /*
                1. Post Spring Database Tasks
                    >> Database Manager
                        a. Encryption Keys
                        b. Incoming Whitelisted IPs
             */
            DatabaseServerConfig.init();
            DatabaseManager.getInstance();

//            NioServer nioServer = new NioServer();
//            nioServer.init();
        }
    }
}
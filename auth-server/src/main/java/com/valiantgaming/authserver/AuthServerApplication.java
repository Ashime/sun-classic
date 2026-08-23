package com.valiantgaming.authserver;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.authserver.server.NioServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j2
@SpringBootApplication
public class AuthServerApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AuthServer.class, args);
    }

    public static class AuthServer
    {
        public AuthServer()
        {
            /*
                1. Post Spring Login Tasks
                    >> Config Manager
                        a. Auth Server Config
                    >> Task Manager
                        a.
                    >> Nio-Server
             */
            AuthServerConfig.getInstance();
            NioServer.getInstance();
        }
    }
}
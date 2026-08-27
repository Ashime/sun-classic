package com.valiantgaming.webserver;

import com.valiantgaming.webserver.config.WebServerConfig;
import com.valiantgaming.webserver.server.NioServer;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@Log4j2
@SpringBootApplication
public class WebServerApplication
{
    public static void main(String[] args)
    {
        WebServerConfig.getInstance();
        // Starts connecting to Database Server on its own background thread - unlike auth-server,
        // nothing here needs to block: Spring's embedded servlet container (started below) is what
        // keeps the JVM alive.
        NioServer.getInstance();

        new SpringApplicationBuilder(WebServerApplication.class)
                .properties("server.port=" + WebServerConfig.getHttpPort())
                .run(args);
    }
}

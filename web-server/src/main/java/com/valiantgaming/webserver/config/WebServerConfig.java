package com.valiantgaming.webserver.config;

import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

public class WebServerConfig
{
    private static WebServerConfig instance;
    private static final String webServerFile = "Config/WebServer/WebServer.ini";

    // [NETWORK]
    @Getter
    private static int disconnect;

    // [TLS]
    @Getter
    private static String tlsCertPath;
    @Getter
    private static String tlsKeyPath;
    @Getter
    private static String tlsCaPath;

    // [DATABASE_SERVER]
    @Getter
    private static String dbServerIp;
    @Getter
    private static int dbServerPort;

    // [SERVER]
    @Getter
    private static String serverIp;
    @Getter
    private static int serverPort;
    @Getter
    private static int serverWorkingThreads;

    // [HTTP]
    @Getter
    private static int httpPort;

    @SneakyThrows
    public WebServerConfig()
    {
        Wini ini = new Wini(new File(webServerFile));

        // [NETWORK]
        disconnect = ini.get("NETWORK", "DISCONNECT", int.class);

        // [TLS]
        tlsCertPath = ini.get("TLS", "CERT_PATH", String.class);
        tlsKeyPath = ini.get("TLS", "KEY_PATH", String.class);
        tlsCaPath = ini.get("TLS", "CA_PATH", String.class);

        // [DATABASE_SERVER]
        dbServerIp = ini.get("DATABASE_SERVER", "IP", String.class);
        dbServerPort = ini.get("DATABASE_SERVER", "PORT", int.class);

        // [SERVER]
        serverIp = ini.get("SERVER", "IP", String.class);
        serverPort = ini.get("SERVER", "PORT", int.class);
        serverWorkingThreads = ini.get("SERVER", "WORKING_THREADS", int.class);

        // [HTTP]
        httpPort = ini.get("HTTP", "PORT", int.class);

        ini.clear();
    }

    public static WebServerConfig getInstance()
    {
        if(instance == null)
            synchronized (WebServerConfig.class)
            {
                if(instance == null)
                    instance = new WebServerConfig();
            }

        return instance;
    }
}

package com.valiantgaming.authserver.config;

import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

public class AuthServerConfig
{
    private static AuthServerConfig instance;
    private static final String authServerFile = "Config/AuthServer/AuthServer.ini";

    // ---------------- Login Server ---------------
    // [VERSION]
    @Getter
    private static String clientVersion;
    @Getter
    private static String clientProtocol;
    @Getter
    private static String launcherVersion;

    // [NETWORK]
    @Getter
    private static boolean uniqueIpFilter;
    @Getter
    private static int disconnect;

    // [SECURITY]
    @Getter
    private static boolean trustedDevices;

    // [DATABASE_SERVER]
    @Getter
    private static String dbServerIp;
    @Getter
    private static int dbServerPort;

    // [CLIENT]
    @Getter
    private static String clientIp;
    @Getter
    private static int clientPort;
    @Getter
    private static int clientAcceptThreads;
    @Getter
    private static int clientWorkingThreads;
    @Getter
    private static int clientBufferSize;

    // [SERVER]
    @Getter
    private static String serverIp;
    @Getter
    private static int serverPort;
    @Getter
    private static int serverAcceptThreads;
    @Getter
    private static int serverWorkingThreads;
    @Getter
    private static int serverBufferSize;

    @SneakyThrows
    public AuthServerConfig()
    {
        Wini ini = new Wini(new File(authServerFile));

        // ---------------- Login Server ---------------
        // [VERSION]
        clientVersion = ini.get("VERSION", "CLIENT_VERSION", String.class);
        clientProtocol = ini.get("VERSION", "CLIENT_PROTOCOL", String.class);
        launcherVersion = ini.get("VERSION", "LAUNCHER_VERSION", String.class);

        // [NETWORK]
        uniqueIpFilter = ini.get("NETWORK", "UNIQUE_IP_FILTER", boolean.class);
        disconnect = ini.get("NETWORK", "DISCONNECT", int.class);

        // [SECURITY]
        trustedDevices = ini.get("SECURITY", "TRUSTED_DEVICES", boolean.class);

        // [DATABASE_SERVER]
        dbServerIp = ini.get("DATABASE_SERVER", "IP", String.class);
        dbServerPort = ini.get("DATABASE_SERVER", "PORT", int.class);

        // [CLIENT]
        clientIp = ini.get("CLIENT", "IP", String.class);
        clientPort = ini.get("CLIENT", "PORT", int.class);
        clientAcceptThreads = ini.get("CLIENT", "ACCEPT_THREADS", int.class);
        clientWorkingThreads = ini.get("CLIENT", "WORKING_THREADS", int.class);
        clientBufferSize = ini.get("CLIENT", "BUFFER_SIZE", int.class);

        // [SERVER]
        serverIp = ini.get("SERVER", "IP", String.class);
        serverPort = ini.get("SERVER", "PORT", int.class);
        serverAcceptThreads = ini.get("SERVER", "ACCEPT_THREADS", int.class);
        serverWorkingThreads = ini.get("SERVER", "WORKING_THREADS", int.class);
        serverBufferSize = ini.get("SERVER", "BUFFER_SIZE", int.class);

        ini.clear();
    }

    public static AuthServerConfig getInstance()
    {
        if(instance == null)
            synchronized (AuthServerConfig.class)
            {
                if(instance == null)
                    instance = new AuthServerConfig();
            }

        return instance;
    }
}
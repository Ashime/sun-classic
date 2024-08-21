package com.valiantgaming.databaseserver.config;

import com.valiantgaming.databaseserver.network.server.NioServer;
import com.valiantgaming.databaseserver.security.crypt.BCRYPT;
import com.valiantgaming.databaseserver.security.hash.SHA;
import org.ini4j.Wini;

import java.io.File;

public class DatabaseServerConfig
{
    private static final String fileName = "./Config/DatabaseServer/DatabaseServer.ini";

    public static void init()
    {
        Wini ini = new Wini();
        ini.setFile(new File(fileName));

        // [SECURITY]
        SHA.setRecreateHmacKey(ini.get("SECURITY", "RECREATE_HMAC_KEY", boolean.class));
        BCRYPT.setLogRounds(ini.get("SECURITY", "BCRYPT_ROUNDS", int.class));

        // [SERVER]
        NioServer.setIpAddress(ini.get("SERVER", "IP_ADDRESS", String.class));
        NioServer.setPort(ini.get("SERVER", "PORT", int.class));
        NioServer.setAcceptThreads(ini.get("SERVER", "ACCEPT_THREADS", int.class));
        NioServer.setWorkingThreads(ini.get("SERVER", "WORKING_THREADS", int.class));
        NioServer.setBufferSize(ini.get("SERVER", "BUFFER_SIZE", int.class));
        NioServer.setConnectionTimeout(ini.get("SERVER", "CONNECTION_TIMEOUT", int.class));

        ini.clear();
    }
}
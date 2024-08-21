package com.valiantgaming.databaseserver.config;

import com.valiantgaming.databaseserver.network.server.NioServer;
import com.valiantgaming.databaseserver.security.crypt.Bcrypt;
import com.valiantgaming.databaseserver.security.hash.SHA512;
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
        SHA512.setRecreateHmacKey(ini.get("SECURITY", "RECREATE_HMAC_KEY", boolean.class));
        Bcrypt.setLogRounds(ini.get("SECURITY", "BCRYPT_ROUNDS", int.class));

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
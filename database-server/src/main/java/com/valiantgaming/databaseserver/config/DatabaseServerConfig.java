package com.valiantgaming.databaseserver.config;

import com.valiantgaming.commons.security.crypt.AES;
import com.valiantgaming.commons.security.crypt.BCRYPT;
import com.valiantgaming.commons.security.hash.SHA;
import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

public class DatabaseServerConfig
{
    private static DatabaseServerConfig instance;
    private static final String dbServerFile = "Config/DatabaseServer/DatabaseServer.ini";

    // -------------- Database Server --------------
    // [NETWORK]
    @Getter
    private static int disconnect;

    // [SECURITY]
    @Getter
    private static int hmacTimestampOffset;

    // [SERVER]
    @Getter
    private static String ipAddress;
    @Getter
    private static int port;
    @Getter
    private static int acceptThreads;
    @Getter
    private static int workingThreads;
    @Getter
    private static int bufferSize;

    // [TLS]
    @Getter
    private static String tlsCertPath;
    @Getter
    private static String tlsKeyPath;
    @Getter
    private static String tlsCaPath;

    @SneakyThrows
    public DatabaseServerConfig()
    {
        Wini ini = new Wini(new File(dbServerFile));

        // -------------- Database Server --------------
        // [NETWORK]
        disconnect = ini.get("NETWORK", "DISCONNECT", int.class);

        // [SECURITY]
        AES.setRecreateAesKey(ini.get("SECURITY", "RECREATE_AES_KEY", boolean.class));
        SHA.setRecreateHmacKey(ini.get("SECURITY", "RECREATE_HMAC_KEY", boolean.class));
        BCRYPT.setLogRounds(ini.get("SECURITY", "BCRYPT_ROUNDS", int.class));
        hmacTimestampOffset = ini.get("SECURITY", "HMAC_TIMESTAMP_OFFSET", int.class);

        // [SERVER]
        ipAddress = ini.get("SERVER", "IP", String.class);
        port = ini.get("SERVER", "PORT", int.class);
        acceptThreads = ini.get("SERVER", "ACCEPT_THREADS", int.class);
        workingThreads = ini.get("SERVER", "WORKING_THREADS", int.class);
        bufferSize = ini.get("SERVER", "BUFFER_SIZE", int.class);

        // [TLS]
        tlsCertPath = ini.get("TLS", "CERT_PATH", String.class);
        tlsKeyPath = ini.get("TLS", "KEY_PATH", String.class);
        tlsCaPath = ini.get("TLS", "CA_PATH", String.class);

        ini.clear();
    }

    public static DatabaseServerConfig getInstance()
    {
        if(instance == null)
            synchronized (DatabaseServerConfig.class)
            {
                if(instance == null)
                    instance = new DatabaseServerConfig();
            }

        return instance;
    }
}
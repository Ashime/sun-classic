package com.valiantgaming.databaseserver.config.dbmanager;

import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

public class DbManagerConfig
{
    private static DbManagerConfig instance;
    private static final String dbManagerFile = "Config/DatabaseServer/DbManager/DatabaseManager.ini";

    // -------------- Database Manager --------------
    // [TASK_EXECUTOR]
    @Getter
    private static int teMinPoolSize;
    @Getter
    private static int teMaxPoolSize;
    @Getter
    private static int teThreadKeepAliveTime;
    @Getter
    private static int teMinTerminationAwaitTime;
    @Getter
    private static int teMaxTerminationAwaitTime;

    // [TASK_SCHEDULER]
    @Getter
    private static int tsMinPoolSize;
    @Getter
    private static int tsAdditionalPoolThreads;
    @Getter
    private static int tsMinTerminationAwaitTime;
    @Getter
    private static int tsMaxTerminationAwaitTime;

    @SneakyThrows
    public DbManagerConfig()
    {
        Wini ini = new Wini(new File(dbManagerFile));

        // -------------- Database Manager --------------
        // [TASK_EXECUTOR]
        teMinPoolSize = ini.get("TASK_EXECUTOR", "MIN_POOL_SIZE", int.class);
        teMaxPoolSize = ini.get("TASK_EXECUTOR", "MAX_POOL_SIZE", int.class);
        teThreadKeepAliveTime = ini.get("TASK_EXECUTOR", "THREAD_KEEP_ALIVE_TIME", int.class);
        teMinTerminationAwaitTime = ini.get("TASK_EXECUTOR", "MIN_TERMINATION_AWAIT_TIME", int.class);
        teMaxTerminationAwaitTime = ini.get("TASK_EXECUTOR", "MAX_TERMINATION_AWAIT_TIME", int.class);

        // [TASK_SCHEDULER]
        tsMinPoolSize = ini.get("TASK_SCHEDULER", "MIN_POOL_SIZE", int.class);
        tsAdditionalPoolThreads = ini.get("TASK_SCHEDULER", "ADDITIONAL_POOL_THREADS", int.class);
        tsMinTerminationAwaitTime = ini.get("TASK_SCHEDULER", "MIN_TERMINATION_AWAIT_TIME", int.class);
        tsMaxTerminationAwaitTime = ini.get("TASK_SCHEDULER", "MAX_TERMINATION_AWAIT_TIME", int.class);

        ini.clear();
    }

    public static DbManagerConfig getInstance()
    {
        if(instance == null)
            synchronized (DbManagerConfig.class)
            {
                if(instance == null)
                    instance = new DbManagerConfig();
            }

        return instance;
    }
}

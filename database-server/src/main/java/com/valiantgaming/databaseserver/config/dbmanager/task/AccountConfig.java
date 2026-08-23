package com.valiantgaming.databaseserver.config.dbmanager.task;

import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

public class AccountConfig
{
    private static AccountConfig instance;
    private static final String accountFile = "Config/DatabaseServer/DbManager/Tasks/Account.ini";

    // [DEACTIVATION]
    @Getter
    private static int accDeactWaitTime;

    // [DELETION]
    @Getter
    private static int accDelInitDelay;
    @Getter
    private static int accDelScheduleTime;
    @Getter
    private static String accDelScheduleTimeUnit;

    @SneakyThrows
    public AccountConfig()
    {
        Wini ini = new Wini(new File(accountFile));

        // -------------- Account Tasks --------------
        // [DEACTIVATION]
        accDeactWaitTime = ini.get("DEACTIVATION", "WAIT_TIME", int.class);

        // [DELETION]
        accDelInitDelay = ini.get("DELETION", "INITIAL_DELAY", int.class);
        accDelScheduleTime = ini.get("DELETION", "SCHEDULE_TIME", int.class);
        accDelScheduleTimeUnit = ini.get("DELETION", "SCHEDULE_TIME_UNIT", String.class);

        ini.clear();
    }
    public static AccountConfig getInstance()
    {
        if(instance == null)
            synchronized (AccountConfig.class)
            {
                if(instance == null)
                    instance = new AccountConfig();
            }

        return instance;
    }
}
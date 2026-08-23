package com.valiantgaming.databaseserver.config.dbmanager.task;

import lombok.Getter;
import lombok.SneakyThrows;
import org.ini4j.Wini;

import java.io.File;

public class CharacterConfig
{
    private static CharacterConfig instance;
    private static final String characterFile = "Config/DatabaseServer/DbManager/Tasks/Character.ini";

    // [DEACTIVATION]
    @Getter
    private static int charDeactWaitTime;

    // [DELETION]
    @Getter
    private static int charDelInitDelay;
    @Getter
    private static int charDelSchedulePeriod;
    @Getter
    private static String charDelSchedulePeriodUnit;

    @SneakyThrows
    public CharacterConfig()
    {
        Wini ini = new Wini(new File(characterFile));

        // -------------- Character Tasks --------------
        // [DEACTIVATION]
        charDeactWaitTime = ini.get("DEACTIVATION", "WAIT_TIME", int.class);

        // [DELETION]
        charDelInitDelay = ini.get("DELETION", "INITIAL_DELAY", int.class);
        charDelSchedulePeriod = ini.get("DELETION", "SCHEDULE_PERIOD", int.class);
        charDelSchedulePeriodUnit = ini.get("DELETION", "SCHEDULE_PERIOD_UNIT", String.class);

        ini.clear();
    }
    public static CharacterConfig getInstance()
    {
        if(instance == null)
            synchronized (CharacterConfig.class)
            {
                if(instance == null)
                    instance = new CharacterConfig();
            }

        return instance;
    }
}
package com.valiantgaming.databaseserver.config;

import com.valiantgaming.databaseserver.config.dbmanager.DbManagerConfig;
import com.valiantgaming.databaseserver.config.dbmanager.task.AccountConfig;
import com.valiantgaming.databaseserver.config.dbmanager.task.CharacterConfig;

public class ConfigManager
{
    private static ConfigManager instance;

    public ConfigManager()
    {
        DatabaseServerConfig.getInstance();
        DbManagerConfig.getInstance();
        AccountConfig.getInstance();
        CharacterConfig.getInstance();
    }
    public static ConfigManager getInstance()
    {
        if(instance == null)
            synchronized (ConfigManager.class)
            {
                if(instance == null)
                    instance = new ConfigManager();
            }

        return instance;
    }
}

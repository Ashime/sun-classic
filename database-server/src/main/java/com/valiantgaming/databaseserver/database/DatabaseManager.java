package com.valiantgaming.databaseserver.database;

import com.valiantgaming.commons.security.crypt.AES;
import com.valiantgaming.commons.security.hash.SHA;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.databaseserver.config.dbmanager.DbManagerConfig;
import com.valiantgaming.databaseserver.config.dbmanager.task.AccountConfig;
import com.valiantgaming.databaseserver.database.dao.EncryptionKeyDAO;
import com.valiantgaming.databaseserver.database.dao.account.AccountDAO;
import com.valiantgaming.databaseserver.database.dao.server.ServerInfoDAO;
import com.valiantgaming.databaseserver.database.entity.EncryptionKey;
import com.valiantgaming.databaseserver.database.entity.account.Account;
import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import com.valiantgaming.databaseserver.network.firewall.IpRules;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

@Log4j2
@Service
public class DatabaseManager
{
    private static DatabaseManager instance;
    private static ExecutorService taskExecutor;
    private static ScheduledExecutorService taskScheduler;

    @SneakyThrows
    private DatabaseManager()
    {
        // --------------------------------------
        //             Start-up Tasks
        // --------------------------------------
        HibernateSession.createSessionFactory();

        EncryptionKeyDAO encKeyDAO = new EncryptionKeyDAO(HibernateSession.createSession());
        AES aes = new AES();

        if(AES.isRecreateAesKey())
        {
            String shaKey = aes.decryptFile(encKeyDAO.getKey("AES-256 KEY").getKeyValue(), encKeyDAO.getKey("AES-256 IV").getKeyValue());

            EncryptionKey aesKey = new EncryptionKey();
            aesKey.setKeyName("AES-256 KEY");
            aesKey.setKeyValue(Utility.byteArrayToHexString(aes.generateKey().getEncoded()));
            aesKey.setActive(true);

            EncryptionKey aesIv = new EncryptionKey();
            aesIv.setKeyName("AES-256 IV");
            aesIv.setKeyValue(Utility.byteArrayToHexString(aes.generateIV()));
            aesIv.setActive(true);

            if(encKeyDAO.addKey(aesKey))
            {
                if(encKeyDAO.addKey(aesIv))
                {
                    aes.encryptFile(aesKey.getKeyValue(), aesIv.getKeyValue(), shaKey.getBytes(StandardCharsets.UTF_8));
                }
                else
                {
                    log.error("FAILED TO RECREATE AES-256 IV!");
                    System.exit(0);
                }
            }
            else
            {
                log.error("FAILED TO RECREATE AES-256 KEY!");
                System.exit(0);
            }
        }

        if(SHA.isRecreateHmacKey())
        {
            AccountDAO accountDAO = new AccountDAO(HibernateSession.createSession());

            aes.encryptFile(encKeyDAO.getKey("AES-256 KEY").getKeyValue(), encKeyDAO.getKey("AES-256 IV").getKeyValue(), SHA.generateHmacKey());
            accountDAO.updateAllPasswords();
        }
        else
        {
            SHA.setHmacKey(aes.decryptFile(encKeyDAO.getKey("AES-256 KEY").getKeyValue(), encKeyDAO.getKey("AES-256 IV").getKeyValue()));
        }

        // --------------------------------------
        //          Start Task Executors
        // --------------------------------------
        taskExecutor = new ThreadPoolExecutor(DbManagerConfig.getTeMinPoolSize(), DbManagerConfig.getTeMaxPoolSize(), DbManagerConfig.getTeThreadKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>());

        taskScheduler = new ScheduledThreadPoolExecutor(DbManagerConfig.getTsMinPoolSize(), (r, executor) -> {
            log.warn("Task Scheduler core pool size has increased from " + executor.getCorePoolSize() + " to " + (executor.getCorePoolSize() + DbManagerConfig.getTsAdditionalPoolThreads()));
            executor.setCorePoolSize(executor.getCorePoolSize() + DbManagerConfig.getTsAdditionalPoolThreads());
            executor.execute(r);
        });

        // -------- One-Time Tasks --------
        // IpRules for NioServer needs the ServerInfo from database.
        IpRules.setServerInfoList((List<ServerInfo>) submitTask(() -> new ServerInfoDAO(HibernateSession.createSession()).getServerInfo()).get());

        // -------- Scheduled Tasks --------
        // Schedule character and account deletion tasks.
        // TODO: Create stored procedure, add NamedStoredProcedure Hibernate syntax in Character.java, and finish method in CharacterDAO.java
        // Delete character task.
//        scheduleTask(() -> {
//            CharacterDAO characterDAO = new CharacterDAO(HibernateSession.createSession());
//
//        }, CharacterConfig.getCharDelInitDelay(), CharacterConfig.getCharDelSchedulePeriod(), CharacterConfig.getCharDelSchedulePeriodUnit());

        // Delete account task.
        // TODO: Finish stored procedure (DeleteAccount).
        scheduleTask(() -> {
            AccountDAO accountDAO = new AccountDAO(HibernateSession.createSession());
            List<Account> results = accountDAO.getDeactivatedAccounts();

            if(!results.isEmpty())
            {
                for(Account a : results)
                {
                    accountDAO.deleteAccount(a.getUsername());
                }
            }
        }, AccountConfig.getAccDelInitDelay(), AccountConfig.getAccDelScheduleTime(), AccountConfig.getAccDelScheduleTimeUnit());
    }

    /*
        EXAMPLE:
        Future<Object> futureTask = DatabaseManager.getInstance().submitTask(() -> new ServerInfoDAO(HibernateSession.createSession()).getServerInfo());
        if(futureTask.get() != null)
            for(ServerInfo s : (List<ServerInfo>) futureTask.get())
                log.info(s.toString());
     */
    public Future<Object> submitTask(Callable<Object> task)
    {
        return taskExecutor.submit(task);
    }

    public void scheduleTask(Runnable task, int initialDelay, int period, String unit)
    {
        taskScheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.valueOf(unit));
    }

    @SneakyThrows
    public void shutdownExecutor()
    {
        int timeAwaited = 0;

        while(!taskExecutor.isShutdown())
        {
            if(!taskExecutor.awaitTermination(DbManagerConfig.getTeMinTerminationAwaitTime(), TimeUnit.SECONDS)
                    || timeAwaited != DbManagerConfig.getTeMaxTerminationAwaitTime())
            {
                timeAwaited += DbManagerConfig.getTeMinTerminationAwaitTime();
                Thread.sleep(DbManagerConfig.getTeMinTerminationAwaitTime());
            }
            else
            {
                taskExecutor.shutdown();
            }
        }
    }

    @SneakyThrows
    public void shutdownScheduler()
    {
        int timeAwaited = 0;

        while(!taskScheduler.isShutdown())
        {
            if(!taskScheduler.awaitTermination(DbManagerConfig.getTsMinTerminationAwaitTime(), TimeUnit.SECONDS)
                    || timeAwaited != DbManagerConfig.getTsMaxTerminationAwaitTime())
            {
                timeAwaited += DbManagerConfig.getTsMinTerminationAwaitTime();
                Thread.sleep(DbManagerConfig.getTsMinTerminationAwaitTime());
            }
            else
            {
                taskScheduler.shutdown();
            }
        }
    }

    public static DatabaseManager getInstance()
    {
        if(instance == null)
            synchronized (DatabaseManager.class)
            {
                if(instance == null)
                    instance = new DatabaseManager();
            }

        return instance;
    }
}
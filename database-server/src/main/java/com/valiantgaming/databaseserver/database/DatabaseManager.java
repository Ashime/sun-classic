package com.valiantgaming.databaseserver.database;

import com.valiantgaming.databaseserver.database.dao.account.AccountDAO;
import com.valiantgaming.databaseserver.database.dao.encryption.EncryptionKeyDAO;
import com.valiantgaming.databaseserver.security.crypt.AES256;
import com.valiantgaming.databaseserver.security.hash.SHA512;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class DatabaseManager
{
    private static DatabaseManager instance;

    private DatabaseManager()
    {
        // Run start-up tasks here.
        HibernateSession.createSessionFactory();

        EncryptionKeyDAO encKeyDAO = new EncryptionKeyDAO(HibernateSession.createSession());
        AES256 aes = new AES256();

        if(SHA512.isRecreateHmacKey())
        {
            AccountDAO accountDAO = new AccountDAO(HibernateSession.createSession());

            aes.encryptFile(encKeyDAO.getKey("AES-256 KEY").getKeyValue(), encKeyDAO.getKey("AES-256 IV").getKeyValue(), SHA512.generateHmacKey());
            accountDAO.updateAllPasswords();
        }
        else
            SHA512.setHmacKey(aes.decryptFile(encKeyDAO.getKey("AES-256 KEY").getKeyValue(), encKeyDAO.getKey("AES-256 IV").getKeyValue()));
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
package com.valiantgaming.databaseserver.database.dao.encryption;

import com.valiantgaming.databaseserver.database.entity.encryption.EncryptionKey;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;

@Log4j2
public class EncryptionKeyDAO
{
    private EncryptionKey encryptionKey;
    private Session session;

    public EncryptionKeyDAO(Session session)
    {
        this.session = session;
    }

    public boolean addKey(EncryptionKey encryptionKey)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("AddEncryptionKey")
                .setParameter("encryptionKey", encryptionKey);

        boolean status = query.execute();

        if(status)
            if(query.getSingleResult().toString().equals("SUCCESS!"))
                return true;

        log.error("SP AddEncryptionKey - " + query.getSingleResult().toString());
        return false;
    }

    public EncryptionKey getKey(String keyName)
    {
        encryptionKey = new EncryptionKey();

        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("GetEncryptionKey")
                .setParameter("keyName", keyName);

        if(query.execute())
            encryptionKey = (EncryptionKey) query.getSingleResult();

        return encryptionKey;
    }
}
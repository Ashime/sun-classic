package com.valiantgaming.databaseserver.database.doa.encryption;

import com.valiantgaming.databaseserver.database.entity.EncryptionKey;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Getter
@Repository
public class EncryptionKeyDAOImpl implements EncryptionKeyDAO
{
    private final EntityManager entityManager;

    @Autowired
    public EncryptionKeyDAOImpl(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public boolean addKey(EncryptionKey encryptionKey)
    {
        boolean status = false;



        return status;
    }

    @Override
    public void getKey(String keyName)
    {

    }
}
package com.valiantgaming.databaseserver.database.doa.encryption;

import com.valiantgaming.databaseserver.database.entity.EncryptionKey;

public interface EncryptionKeyDAO
{
    boolean addKey(EncryptionKey encryptionKey);
    void getKey(String keyName);
}
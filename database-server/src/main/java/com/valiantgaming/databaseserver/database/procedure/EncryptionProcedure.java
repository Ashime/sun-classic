package com.valiantgaming.databaseserver.database.procedure;

import com.valiantgaming.databaseserver.database.doa.encryption.EncryptionKeyDAO;
import com.valiantgaming.databaseserver.database.entity.EncryptionKey;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

@Log4j2
public class EncryptionProcedure
{
    @NamedStoredProcedureQueries
    ({
        @NamedStoredProcedureQuery
        (
            name = "AddEncryptionKey",
            procedureName = "AddEncryptionKey",
            resultClasses = { boolean.class },
            parameters =
            {
                @StoredProcedureParameter
                (
                    name = "encryptionKey",
                    type = EncryptionKey.class,
                    mode = ParameterMode.IN
                )
            }
        ),
        @NamedStoredProcedureQuery
        (
            name = "GetEncryptionKey",
            procedureName = "GetEncryptionKey",
            resultClasses = { EncryptionKey.class },
            parameters =
            {
                @StoredProcedureParameter
                (
                    name = "keyName",
                    type = String.class,
                    mode = ParameterMode.IN
                )
            }
        )
    })
    public static class EncryptionKeyProcedure
    {
        private final EncryptionKeyDAO encryptionKeyDAO;

        @Autowired
        public EncryptionKeyProcedure(EncryptionKeyDAO encryptionKeyDAO)
        {
            this.encryptionKeyDAO = encryptionKeyDAO;
        }

        public void addEncryptionKey(EncryptionKey encryptionKey)
        {
            encryptionKeyDAO.addKey(encryptionKey);
        }
    }
}

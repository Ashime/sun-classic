package com.valiantgaming.databaseserver.database.entity.encryption;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NamedStoredProcedureQueries
({
        @NamedStoredProcedureQuery
        (
            name = "AddEncryptionKey",
            procedureName = "AddEncryptionKey",
            resultClasses = String.class,
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
            resultClasses = EncryptionKey.class,
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
@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class EncryptionKey
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "KeyID")
    private int keyID;

    @NotBlank
    @Column(name = "Name")
    private String keyName;

    @NotBlank
    @Column(name = "Value")
    private String keyValue;

    @NotNull
    @Column(name = "IsActive")
    private boolean active;

    @NotNull
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "EncryptionKey{" +
                "keyID=" + keyID +
                ", keyName='" + keyName + '\'' +
                ", keyValue='" + keyValue + '\'' +
                ", active=" + active +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
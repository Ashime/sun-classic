package com.valiantgaming.databaseserver.database.entity.account.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class AccountStorage
{
    @Id
    @Column(name = "AccountStorageID")
    private int accountStorageID;

    @NotBlank
    @Column(name = "TabID1")
    private int tabID1;

    @NotBlank
    @Column(name = "TabID2")
    private int tabID2;

    @NotBlank
    @Column(name = "TabID3")
    private int tabID3;

    @NotBlank
    @Column(name = "TabID4")
    private int tabID4;

    @NotBlank
    @Column(name = "TabID5")
    private int tabID5;

    @NotNull
    @Column(name = "Heim")
    private long heim;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "AccountStorage{" +
                "accountStorageID=" + accountStorageID +
                ", tabID1=" + tabID1 +
                ", tabID2=" + tabID2 +
                ", tabID3=" + tabID3 +
                ", tabID4=" + tabID4 +
                ", tabID5=" + tabID5 +
                ", heim=" + heim +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
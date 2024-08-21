package com.valiantgaming.databaseserver.database.entity.account.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
    @Column(name = "AccountID")
    private int accountID;

    @NotBlank
    @Column(name = "TabNumber")
    private short tabNumber;

    @NotBlank
    @Column(name = "RowID1")
    private int rowID1;

    @NotBlank
    @Column(name = "RowID2")
    private int rowID2;

    @NotBlank
    @Column(name = "RowID3")
    private int rowID3;

    @NotBlank
    @Column(name = "RowID4")
    private int rowID4;

    @NotBlank
    @Column(name = "RowID5")
    private int rowID5;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Override
    public String toString()
    {
        return "AccountStorage{" +
                "accountID=" + accountID +
                ", tabNumber=" + tabNumber +
                ", rowID1=" + rowID1 +
                ", rowID2=" + rowID2 +
                ", rowID3=" + rowID3 +
                ", rowID4=" + rowID4 +
                ", rowID5=" + rowID5 +
                ", createDate=" + createDate +
                '}';
    }
}
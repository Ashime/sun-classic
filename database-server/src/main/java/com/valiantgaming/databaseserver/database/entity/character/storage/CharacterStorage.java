package com.valiantgaming.databaseserver.database.entity.character.storage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class CharacterStorage
{
    @Id
    @Column(name = "CharacterStorageID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int characterStorageID;

    @Column(name = "RowID1")
    private int rowID1;

    @Column(name = "RowID2")
    private int rowID2;

    @Column(name = "RowID3")
    private int rowID3;

    @Column(name = "RowID4")
    private int rowID4;

    @Column(name = "RowID5")
    private int rowID5;

    @Column(name = "RowID6")
    private int rowID6;

    @Column(name = "RowID7")
    private int rowID7;

    @Column(name = "RowID8")
    private int rowID8;

    @Column(name = "RowID9")
    private int rowID9;

    @Column(name = "RowID10")
    private int rowID10;

    @Column(name = "RowID11")
    private int rowID11;

    @Column(name = "RowID12")
    private int rowID12;

    @Column(name = "RowID13")
    private int rowID13;

    @Column(name = "RowID14")
    private int rowID14;

    @Column(name = "RowID15")
    private int rowID15;

    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Override
    public String toString()
    {
        return "CharacterStorage{" +
                "characterStorageID=" + characterStorageID +
                ", rowID1=" + rowID1 +
                ", rowID2=" + rowID2 +
                ", rowID3=" + rowID3 +
                ", rowID4=" + rowID4 +
                ", rowID5=" + rowID5 +
                ", rowID6=" + rowID6 +
                ", rowID7=" + rowID7 +
                ", rowID8=" + rowID8 +
                ", rowID9=" + rowID9 +
                ", rowID10=" + rowID10 +
                ", rowID11=" + rowID11 +
                ", rowID12=" + rowID12 +
                ", rowID13=" + rowID13 +
                ", rowID14=" + rowID14 +
                ", rowID15=" + rowID15 +
                ", createDate=" + createDate +
                '}';
    }
}
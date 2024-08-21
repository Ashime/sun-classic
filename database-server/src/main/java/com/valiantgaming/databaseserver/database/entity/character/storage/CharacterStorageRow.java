package com.valiantgaming.databaseserver.database.entity.character.storage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class CharacterStorageRow
{
    @Id
    @Column(name = "RowID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int rowID;

    @Column(name = "Slot1")
    private int slot1;

    @Column(name = "Slot2")
    private int slot2;

    @Column(name = "Slot3")
    private int slot3;

    @Column(name = "Slot4")
    private int slot4;

    @Column(name = "Slot5")
    private int slot5;

    @Column(name = "Slot6")
    private int slot6;

    @Column(name = "Slot7")
    private int slot7;

    @Column(name = "Slot8")
    private int slot8;

    @Column(name = "Slot9")
    private int slot9;

    @Column(name = "Slot10")
    private int slot10;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;
}
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

    @Column(name = "SlotID1")
    private int slotID1;

    @Column(name = "SlotID2")
    private int slotID2;

    @Column(name = "SlotID3")
    private int slotID3;

    @Column(name = "SlotID4")
    private int slotID4;

    @Column(name = "SlotID5")
    private int slotID5;

    @Column(name = "SlotID6")
    private int slotID6;

    @Column(name = "SlotID7")
    private int slotID7;

    @Column(name = "SlotID8")
    private int slotID8;

    @Column(name = "SlotID9")
    private int slotID9;

    @Column(name = "SlotID10")
    private int slotID10;

    @Column(name = "CreateDate")
    private LocalDateTime createDate;
}
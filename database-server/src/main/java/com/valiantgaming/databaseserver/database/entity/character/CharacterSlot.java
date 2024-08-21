package com.valiantgaming.databaseserver.database.entity.character;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class CharacterSlot
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "CharacterSlotID")
    private int characterSlotID;

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

    @NotNull
    @Column(name = "IsSlot7Locked")
    private boolean slot7locked;

    @NotNull
    @Column(name = "IsSlot8Locked")
    private boolean slot8locked;

    @NotNull
    @Column(name = "IsSlot9Locked")
    private boolean slot9locked;

    @NotNull
    @Column(name = "IsSlot10Locked")
    private boolean slot10locked;

    @NotNull
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "CharacterSlot{" +
                "characterSlotID=" + characterSlotID +
                ", slot1=" + slot1 +
                ", slot2=" + slot2 +
                ", slot3=" + slot3 +
                ", slot4=" + slot4 +
                ", slot5=" + slot5 +
                ", slot6=" + slot6 +
                ", slot7=" + slot7 +
                ", slot8=" + slot8 +
                ", slot9=" + slot9 +
                ", slot10=" + slot10 +
                ", slot7locked=" + slot7locked +
                ", slot8locked=" + slot8locked +
                ", slot9locked=" + slot9locked +
                ", slot10locked=" + slot10locked +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
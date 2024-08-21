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
    @Column(name = "AccountID")
    private int accountID;

    @Column(name = "CharacterSlot1")
    private int characterSlot1;

    @Column(name = "CharacterSlot2")
    private int characterSlot2;

    @Column(name = "CharacterSlot3")
    private int characterSlot3;

    @Column(name = "CharacterSlot4")
    private int characterSlot4;

    @Column(name = "CharacterSlot5")
    private int characterSlot5;

    @Column(name = "CharacterSlot6")
    private int characterSlot6;

    @Column(name = "CharacterSlot7")
    private int characterSlot7;

    @Column(name = "CharacterSlot8")
    private int characterSlot8;

    @Column(name = "CharacterSlot9")
    private int characterSlot9;

    @Column(name = "CharacterSlot10")
    private int characterSlot10;

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
    public String toString() {
        return "CharacterSlot{" +
                "accountID=" + accountID +
                ", characterSlot1=" + characterSlot1 +
                ", characterSlot2=" + characterSlot2 +
                ", characterSlot3=" + characterSlot3 +
                ", characterSlot4=" + characterSlot4 +
                ", characterSlot5=" + characterSlot5 +
                ", characterSlot6=" + characterSlot6 +
                ", characterSlot7=" + characterSlot7 +
                ", characterSlot8=" + characterSlot8 +
                ", characterSlot9=" + characterSlot9 +
                ", characterSlot10=" + characterSlot10 +
                ", slot7locked=" + slot7locked +
                ", slot8locked=" + slot8locked +
                ", slot9locked=" + slot9locked +
                ", slot10locked=" + slot10locked +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
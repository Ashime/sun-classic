package com.valiantgaming.databaseserver.database.entity.character;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class Character
{
    @Id
    @Column(name = "CharacterID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int characterID;

    @NotNull
    @Column(name = "CharacterStorageID")
    private int characterStorageID;

    @NotNull
    @Column(name = "EquipmentID")
    private int equipmentID;

    @NotNull
    @Column(name = "InventoryID")
    private int inventoryID;

    @NotNull
    @Column(name = "SkillInventoryID")
    private int skillInventoryID;

    @NotBlank
    @Length(min = 4, max = 25)
    @Column(name = "Name")
    private String characterName;

    @NotNull
    @Column(name = "Level")
    private short level;

    @NotBlank
    @Column(name = "Class")
    private String className;

    @NotBlank
    @Column(name = "Race")
    private String race;

    // Male = 2 and Female = 1, 0 = NA
    @NotBlank
    @Column(name = "Gender")
    private short gender;

    @NotNull
    @Column(name = "SD")
    private short sd;

    @NotNull
    @Column(name = "MaxSD")
    private short maxSD;

    @NotNull
    @Column(name = "HP")
    private short hp;

    @NotNull
    @Column(name = "MaxHP")
    private short maxHP;

    @NotNull
    @Column(name = "MP")
    private short mp;

    @NotNull
    @Column(name = "MaxMP")
    private short maxMP;

    @NotNull
    @Column(name = "STR")
    private short strength;

    @NotNull
    @Column(name = "AGI")
    private short agility;

    @NotNull
    @Column(name = "VIT")
    private short vitality;

    @NotNull
    @Column(name = "INT")
    private short intellect;

    @NotNull
    @Column(name = "SPI")
    private short spirit;

    @NotNull
    @Column(name = "RemainStatPoints")
    private short remainStatPoints;

    @NotNull
    @Column(name = "UsedStatPoints")
    private short usedStatPoints;

    @NotNull
    @Column(name = "RemainSkillPoints")
    private short remainSkillPoints;

    @NotNull
    @Column(name = "RemainResets")
    private short remainResets;

    @NotNull
    @Column(name = "IsInDeletion")
    private boolean inDeletion;

    @NotBlank
    @Column(name = "DeletionDate")
    private LocalDateTime deletionDate;

    @NotNull
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "Character{" +
                "characterID=" + characterID +
                ", characterStorageID=" + characterStorageID +
                ", equipmentID=" + equipmentID +
                ", inventoryID=" + inventoryID +
                ", skillInventoryID=" + skillInventoryID +
                ", characterName='" + characterName + '\'' +
                ", level=" + level +
                ", className='" + className + '\'' +
                ", race='" + race + '\'' +
                ", gender=" + gender +
                ", sd=" + sd +
                ", maxSD=" + maxSD +
                ", hp=" + hp +
                ", maxHP=" + maxHP +
                ", mp=" + mp +
                ", maxMP=" + maxMP +
                ", strength=" + strength +
                ", agility=" + agility +
                ", vitality=" + vitality +
                ", intellect=" + intellect +
                ", spirit=" + spirit +
                ", remainStatPoints=" + remainStatPoints +
                ", usedStatPoints=" + usedStatPoints +
                ", remainSkillPoints=" + remainSkillPoints +
                ", remainResets=" + remainResets +
                ", inDeletion=" + inDeletion +
                ", deletionDate=" + deletionDate +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
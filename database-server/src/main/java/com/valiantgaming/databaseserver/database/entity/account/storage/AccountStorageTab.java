package com.valiantgaming.databaseserver.database.entity.account.storage;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class AccountStorageTab
{
    @Id
    @Column(name = "TabID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int tabID;

    @NotBlank
    @Column(name = "TabNumber")
    private short tabNumber;

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

    @Column(name = "Slot11")
    private int slot11;

    @Column(name = "Slot12")
    private int slot12;

    @Column(name = "Slot13")
    private int slot13;

    @Column(name = "Slot14")
    private int slot14;

    @Column(name = "Slot15")
    private int slot15;

    @Column(name = "Slot16")
    private int slot16;

    @Column(name = "Slot17")
    private int slot17;

    @Column(name = "Slot18")
    private int slot18;

    @Column(name = "Slot19")
    private int slot19;

    @Column(name = "Slot20")
    private int slot20;

    @Column(name = "Slot21")
    private int slot21;

    @Column(name = "Slot22")
    private int slot22;

    @Column(name = "Slot23")
    private int slot23;

    @Column(name = "Slot24")
    private int slot24;

    @Column(name = "Slot25")
    private int slot25;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "AccountStorageTab{" +
                "tabID=" + tabID +
                ", tabNumber=" + tabNumber +
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
                ", slot11=" + slot11 +
                ", slot12=" + slot12 +
                ", slot13=" + slot13 +
                ", slot14=" + slot14 +
                ", slot15=" + slot15 +
                ", slot16=" + slot16 +
                ", slot17=" + slot17 +
                ", slot18=" + slot18 +
                ", slot19=" + slot19 +
                ", slot20=" + slot20 +
                ", slot21=" + slot21 +
                ", slot22=" + slot22 +
                ", slot23=" + slot23 +
                ", slot24=" + slot24 +
                ", slot25=" + slot25 +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
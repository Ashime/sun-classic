package com.valiantgaming.databaseserver.database.entity.inventory;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class InventoryRow
{
    @Id
    @Column(name = "RowID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int rowID;

    @NotBlank
    @Column(name = "RowNumber")
    private short rowNumber;

    @NotBlank
    @Column(name = "Slot1")
    private int slot1;

    @NotBlank
    @Column(name = "Slot2")
    private int slot2;

    @NotBlank
    @Column(name = "Slot3")
    private int slot3;

    @NotBlank
    @Column(name = "Slot4")
    private int slot4;

    @NotBlank
    @Column(name = "Slot5")
    private int slot5;

    @NotBlank
    @Column(name = "Slot6")
    private int slot6;

    @NotBlank
    @Column(name = "Slot7")
    private int slot7;

    @NotBlank
    @Column(name = "Slot8")
    private int slot8;

    @NotBlank
    @Column(name = "Slot9")
    private int slot9;

    @NotBlank
    @Column(name = "Slot10")
    private int slot10;

    @NotBlank
    @Column(name = "IsLocked")
    private boolean locked;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "InventoryRow{" +
                "rowID=" + rowID +
                ", rowNumber=" + rowNumber +
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
                ", locked=" + locked +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
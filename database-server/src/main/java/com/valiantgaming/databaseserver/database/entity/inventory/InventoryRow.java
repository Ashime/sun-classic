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
    @Column(name = "Number")
    private short rowNumber;

    @NotBlank
    @Column(name = "SlotID1")
    private int slotID1;

    @NotBlank
    @Column(name = "SlotID2")
    private int slotID2;

    @NotBlank
    @Column(name = "SlotID3")
    private int slotID3;

    @NotBlank
    @Column(name = "SlotID4")
    private int slotID4;

    @NotBlank
    @Column(name = "SlotID5")
    private int slotID5;

    @NotBlank
    @Column(name = "SlotID6")
    private int slotID6;

    @NotBlank
    @Column(name = "SlotID7")
    private int slotID7;

    @NotBlank
    @Column(name = "SlotID8")
    private int slotID8;

    @NotBlank
    @Column(name = "SlotID9")
    private int slotID9;

    @NotBlank
    @Column(name = "SlotID10")
    private int slotID10;

    @NotBlank
    @Column(name = "IsLocked")
    private boolean locked;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "InventoryRow{" +
                "rowID=" + rowID +
                ", rowNumber=" + rowNumber +
                ", slotID1=" + slotID1 +
                ", slotID2=" + slotID2 +
                ", slotID3=" + slotID3 +
                ", slotID4=" + slotID4 +
                ", slotID5=" + slotID5 +
                ", slotID6=" + slotID6 +
                ", slotID7=" + slotID7 +
                ", slotID8=" + slotID8 +
                ", slotID9=" + slotID9 +
                ", slotID10=" + slotID10 +
                ", locked=" + locked +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
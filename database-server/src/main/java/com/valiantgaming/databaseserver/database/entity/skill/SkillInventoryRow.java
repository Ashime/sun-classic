package com.valiantgaming.databaseserver.database.entity.skill;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class SkillInventoryRow
{
    @Id
    @Column(name = "RowID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int rowID;

    @NotNull
    @Column(name = "Number")
    private short rowNumber;

    @NotNull
    @Column(name = "SlotID1")
    private int slotID1;

    @NotNull
    @Column(name = "SlotID2")
    private int slotID2;

    @NotNull
    @Column(name = "SlotID3")
    private int slotID3;

    @NotNull
    @Column(name = "SlotID4")
    private int slotID4;

    @NotNull
    @Column(name = "SlotID5")
    private int slotID5;

    @NotNull
    @Column(name = "SlotID6")
    private int slotID6;

    @NotNull
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Override
    public String toString() {
        return "SkillInventoryRow{" +
                "rowID=" + rowID +
                ", rowNumber=" + rowNumber +
                ", slotID1=" + slotID1 +
                ", slotID2=" + slotID2 +
                ", slotID3=" + slotID3 +
                ", slotID4=" + slotID4 +
                ", slotID5=" + slotID5 +
                ", slotID6=" + slotID6 +
                ", createDate=" + createDate +
                '}';
    }
}
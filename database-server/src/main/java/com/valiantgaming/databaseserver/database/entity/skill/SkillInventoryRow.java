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
    @Column(name = "RowNumber")
    private short rowNumber;

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

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "SkillInventoryRow{" +
                "rowID=" + rowID +
                ", rowNumber=" + rowNumber +
                ", slot1=" + slot1 +
                ", slot2=" + slot2 +
                ", slot3=" + slot3 +
                ", slot4=" + slot4 +
                ", slot5=" + slot5 +
                ", slot6=" + slot6 +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
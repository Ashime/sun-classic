package com.valiantgaming.databaseserver.database.entity.skill;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class SkillInventory
{
    @Id
    @Column(name = "SkillInventoryID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int skillInventoryID;

    @NotBlank
    @Column(name = "RowID1")
    private int rowID1;

    @NotBlank
    @Column(name = "RowID2")
    private int rowID2;

    @NotBlank
    @Column(name = "RowID3")
    private int rowID3;

    @NotBlank
    @Column(name = "RowID4")
    private int rowID4;

    @NotBlank
    @Column(name = "RowID5")
    private int rowID5;

    @NotBlank
    @Column(name = "RowID6")
    private int rowID6;

    @NotBlank
    @Column(name = "RowID7")
    private int rowID7;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Override
    public String toString()
    {
        return "SkillInventory{" +
                "skillInventoryID=" + skillInventoryID +
                ", rowID1=" + rowID1 +
                ", rowID2=" + rowID2 +
                ", rowID3=" + rowID3 +
                ", rowID4=" + rowID4 +
                ", rowID5=" + rowID5 +
                ", rowID6=" + rowID6 +
                ", rowID7=" + rowID7 +
                ", createDate=" + createDate +
                '}';
    }
}
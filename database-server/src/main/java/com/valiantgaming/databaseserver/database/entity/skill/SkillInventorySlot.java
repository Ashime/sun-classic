package com.valiantgaming.databaseserver.database.entity.skill;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@NoArgsConstructor
@Getter @Setter
public class SkillInventorySlot
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "SlotID")
    private int slotID;

    @NotNull
    @Column(name = "SkillID")
    private int skillID;

    @NotNull
    @Column(name = "Level")
    private short skillLevel;

    @NotNull
    @Column(name = "MaxLevel")
    private short maxLevel;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "SkillInventorySlot{" +
                "slotID=" + slotID +
                ", skillID=" + skillID +
                ", skillLevel=" + skillLevel +
                ", maxLevel=" + maxLevel +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
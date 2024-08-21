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
public class AccountStorageRow
{
    @Id
    @Column(name = "RowID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int rowID;

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
    @Column(name = "CreateDate")
    private LocalDateTime createDate;
}
package com.valiantgaming.databaseserver.database.entity.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class InventorySlot
{
    @Id
    @Column(name = "SlotID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int slotID;

    @Column(name = "ItemID")
    private int itemID;

    @Column(name = "Quantity")
    private short quantity;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "InventorySlot{" +
                "slotID=" + slotID +
                ", itemID=" + itemID +
                ", quantity=" + quantity +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
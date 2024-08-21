package com.valiantgaming.databaseserver.database.entity.character.equipment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class Equipment
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "EquipmentID")
    private int equipmentID;

    @Column(name = "WeaponSlot")
    private int weaponSlot;

    @Column(name = "HelmetSlot")
    private int helmetSlot;

    @Column(name = "ProtectorSlot")
    private int protectorSlot;

    @Column(name = "ArmorSlot")
    private int armorSlot;

    @Column(name = "ShirtSlot")
    private int shirtSlot;

    @Column(name = "GlovesSlot")
    private int glovesSlot;

    @Column(name = "BeltSlot")
    private int beltSlot;

    @Column(name = "PantsSlot")
    private int pantsSlot;

    @Column(name = "BootsSlot")
    private int bootsSlot;

    @Column(name = "NecklaceSlot")
    private int necklaceSlot;

    @Column(name = "RingSlot1")
    private int ringSlot1;

    @Column(name = "RingSlot2")
    private int ringSlot2;

    @Column(name = "SpAccessorySlot1")
    private int spAccessorySlot1;

    @Column(name = "PSpAccessorySlot2")
    private int spAccessorySlot2;

    @Column(name = "SpAccessorySlot3")
    private int spAccessorySlot3;

    @Column(name = "ModifiedDate")
    private int modifiedDate;

    @Override
    public String toString() {
        return "Equipment{" +
                "equipmentID=" + equipmentID +
                ", weaponSlot=" + weaponSlot +
                ", helmetSlot=" + helmetSlot +
                ", protectorSlot=" + protectorSlot +
                ", armorSlot=" + armorSlot +
                ", shirtSlot=" + shirtSlot +
                ", glovesSlot=" + glovesSlot +
                ", beltSlot=" + beltSlot +
                ", pantsSlot=" + pantsSlot +
                ", bootsSlot=" + bootsSlot +
                ", necklaceSlot=" + necklaceSlot +
                ", ringSlot1=" + ringSlot1 +
                ", ringSlot2=" + ringSlot2 +
                ", spAccessorySlot1=" + spAccessorySlot1 +
                ", spAccessorySlot2=" + spAccessorySlot2 +
                ", spAccessorySlot3=" + spAccessorySlot3 +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}
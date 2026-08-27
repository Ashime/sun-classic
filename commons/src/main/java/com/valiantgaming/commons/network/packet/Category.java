package com.valiantgaming.commons.network.packet;

public interface Category
{
    byte DATABASE = 0x31;
    // TODO: NEED TO CHECK GAME CATEGORY!
    byte GAME = 0x00;
    byte AUTH = 0x33; // Hex value for 3.

    byte LAUNCHER = 0x4C; // Hex value for L.
    byte WEBSITE = 0x57; // Hex value for W.

}
package com.valiantgaming.commons.network.packet;

public interface Category
{
    byte DATABASE = 0x31;
    // TODO: NEED TO CHECK GAME CATEGORY!
    byte GAME = 0x00;
    byte LOGIN = 0x33;
    byte WEBSITE = 0x34;
    byte LAUNCHER = 0x35;
}
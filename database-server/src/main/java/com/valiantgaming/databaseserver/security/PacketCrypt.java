package com.valiantgaming.databaseserver.security;

public class PacketCrypt
{
    // Server-to-Server
    public static byte[] decryptPacket(byte[] input)
    {
        // decrypt packet with RSA.
        // decrypt message with AES256.
        return new byte[]{0x00};
    }
}

package com.valiantgaming.webserver.security;

public class PacketCrypt
{
    // Server-to-Server
    public static byte[] decryptPacket(byte[] rsaKey, byte[] message)
    {
        return new byte[]{0x00};
    }

    public static byte[] encryptPacket(byte[] rsaKey, byte[] message)
    {
        return new byte[]{0x00};
    }

    public static byte[] decryptMessage(byte[] aesKey, byte[] aesIv, byte[] message)
    {
        return new byte[]{0x00};
    }

    public static byte[] encryptMessage(byte[] aesKey, byte[] aesIv, byte[] message)
    {
        return new byte[] {0x00};
    }
}

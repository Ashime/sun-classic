package com.valiantgaming.authserver.network.packet.client;

public class AnsAuthUser
{
    public static byte[] createPacket(byte[] input, byte[] encKey, String ipAddress)
    {
//        byte message;
//
//        if (AuthUser.authUserAndPassword(input, encKey, ipAddress))
//            message = 0x00;
//        else
//            message = 0x01;
//
//        return MessageEncoder.createShortPacket(Category.LOGIN, Protocol.S2C_ansAuthUser, message);

        return new byte[] {0x00};
    }
}
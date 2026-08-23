package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.authserver.network.packet.client.handler.AuthUser;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

public class AnsAuthUser
{
    public static byte[] createPacket(byte[] input, byte[] encKey, String ipAddress)
    {
        byte message;

        if (AuthUser.authUserAndPassword(input, encKey, ipAddress))
            message = 0x00;
        else
            message = 0x01;

        return new byte[] { Category.AUTH, Protocol.A2U_ansAuthUser, message };
    }
}
package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

public class AnsAuthUser
{
    public static byte[] createPacket(boolean authenticated)
    {
        byte message = authenticated ? (byte) 0x00 : (byte) 0x01;

        return new byte[] { Category.AUTH, Protocol.A2U_ansAuthUser, message };
    }
}

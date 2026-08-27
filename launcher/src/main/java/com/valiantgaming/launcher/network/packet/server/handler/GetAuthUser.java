package com.valiantgaming.launcher.network.packet.server.handler;

/**
 * Decodes {@code A2U_ansAuthUser} - see {@code AnsAuthUser} on the server side for the byte
 * convention (0x00 = authenticated, anything else = rejected).
 */
public class GetAuthUser
{
    public static boolean decode(byte[] message)
    {
        return message[2] == 0x00;
    }
}

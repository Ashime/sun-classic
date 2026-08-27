package com.valiantgaming.launcher.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.security.crypt.TEA;

import java.nio.charset.StandardCharsets;

/**
 * Builds {@code U2A_askAuthUser}. Byte layout matches {@code auth-server}'s {@code AuthUser}
 * decoder exactly (see its class comment for the same unconfirmed-against-a-real-capture
 * caveat): 4 bytes of unused filler, a 50-byte null-padded username, 1 filler byte, then a
 * 23-byte password field (only the first 16 bytes of which {@link TEA#passwordDecode} reads -
 * the rest is padding to match the field width).
 */
public class AskAuthUser
{
    private static final int USERNAME_OFFSET = 6;
    private static final int USERNAME_LENGTH = 50;
    private static final int PASSWORD_OFFSET = 57;
    private static final int PASSWORD_FIELD_LENGTH = 23;
    private static final int PACKET_LENGTH = PASSWORD_OFFSET + PASSWORD_FIELD_LENGTH;

    public byte[] createPacket(String username, String password, byte[] teaKey)
    {
        byte[] packet = new byte[PACKET_LENGTH];
        packet[0] = Category.AUTH;
        packet[1] = Protocol.U2A_askAuthUser;

        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(usernameBytes, 0, packet, USERNAME_OFFSET, Math.min(usernameBytes.length, USERNAME_LENGTH));

        byte[] encryptedPassword = TEA.passwordEncode(password, teaKey);
        System.arraycopy(encryptedPassword, 0, packet, PASSWORD_OFFSET, encryptedPassword.length);

        return packet;
    }
}

package com.valiantgaming.authserver.network.packet.client.handler;

import com.valiantgaming.commons.security.crypt.TEA;
import com.valiantgaming.commons.utility.Utility;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Decodes {@code U2A_askAuthUser}'s username/password. Byte offsets below match the layout
 * captured in {@code Protocol}'s {@code U2A_askAuthUser} comment - though that capture's own
 * encrypted-password field looks longer (~35 bytes, and printable-ASCII-shaped) than the 16 raw
 * bytes {@link TEA} decrypts, so this offset/length pairing is still unverified against a real
 * capture.
 *
 * <p>Credential validation itself happens on database-server via the {@code AuthenticateAccount}
 * stored procedure (see {@code AccountDAO.authenticateAccount}), reached over a
 * {@code S2S_askAuthUser}/{@code S2S_ansAuthUser} round trip - see {@code ClientPacketHandler}'s
 * {@code U2A_askAuthUser} case and {@code PendingAuthRequests} for how that asynchronous reply
 * is correlated back to this client's own channel.
 */
public class AuthUser
{
    public record Credentials(String username, String password) {}

    public static Credentials decode(byte[] input, byte[] key)
    {
        byte[] usernameBytes = Utility.cutTail(Arrays.copyOfRange(input, 6, 56));
        byte[] passwordBytes = Arrays.copyOfRange(input, 57, 80);

        byte[] decryptedPassword = Utility.cutTail(TEA.passwordDecode(passwordBytes, key));

        return new Credentials(
                new String(usernameBytes, StandardCharsets.UTF_8),
                new String(decryptedPassword, StandardCharsets.UTF_8)
        );
    }
}

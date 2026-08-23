package com.valiantgaming.authserver.network.packet.client.handler;

import java.util.Arrays;

/**
 * Validates {@code U2A_askAuthUser}'s username/password against the account database.
 *
 * <p>Byte offsets below match the layout captured in {@code Protocol}'s
 * {@code U2A_askAuthUser} comment. Still a stub: real validation needs (1)
 * {@link com.valiantgaming.commons.security.crypt.TEA} to decrypt {@code password} with
 * {@code key} (TEA is currently an empty class) and (2) an S2S packet to database-server to
 * check the decrypted credentials, since auth-server never queries SQL Server directly (see
 * CLAUDE.md) - no such packet exists yet, nor does an {@code AccountDAO} lookup-by-credentials
 * method. {@code AuthServerConfig.isTrustedDevices()}/{@code getHmacTimestampOffset()} are the
 * real config this should read once the trusted-devices check is implemented, replacing the
 * {@code ipAddress} parameter's previous (unimplemented) role.
 */
public class AuthUser
{
    public static boolean authUserAndPassword(byte[] input, byte[] key, String ipAddress)
    {
        byte[] username = Arrays.copyOfRange(input, 6, 56);
        byte[] password = Arrays.copyOfRange(input, 57, 80);

        // TODO: decrypt `password` with TEA using `key`, then ask database-server (via a new
        // S2S packet) to validate the decrypted username/password.
        return false;
    }
}
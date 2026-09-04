package com.valiantgaming.authserver.network.packet.client.launcher;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2L_ansVerifyVersion}, sent right after {@code A2L_ansReady} in reply to the
 * launcher's {@code L2A_askUnknown1}.
 *
 * <p>Layout matches the captured example in {@code Protocol}'s block comment: 4 bytes each
 * for launcher version, client version, and a trailing "unknown" field (guessed there to be
 * the client protocol, but that doesn't line up with {@code AuthServerConfig}'s
 * {@code CLIENT_PROTOCOL} format - see {@code GetVerifyVersion} on the launcher side, which
 * decodes this same layout). Each version component (e.g. "1.0.9.2") becomes one byte, read
 * from {@code AuthServerConfig}'s {@code [VERSION]} section so it always matches what
 * {@code AuthServer.ini} actually declares.
 */
public class AnsVerifyVersion
{
    public byte[] createPacket()
    {
        byte[] launcherVersion = toVersionBytes(AuthServerConfig.getLauncherVersion());
        byte[] clientVersion = toVersionBytes(AuthServerConfig.getClientVersion());
        byte[] unknown = new byte[4]; // TODO: unconfirmed - see this class's comment above.

        byte[] packet = new byte[2 + launcherVersion.length + clientVersion.length + unknown.length];
        packet[0] = Category.AUTH;
        packet[1] = Protocol.A2L_ansVerifyVersion;
        System.arraycopy(launcherVersion, 0, packet, 2, launcherVersion.length);
        System.arraycopy(clientVersion, 0, packet, 6, clientVersion.length);
        System.arraycopy(unknown, 0, packet, 10, unknown.length);

        return packet;
    }

    private static byte[] toVersionBytes(String dottedVersion)
    {
        String[] parts = dottedVersion.split("\\.");
        byte[] version = new byte[4];

        for(int i = 0; i < version.length; i++)
        {
            version[i] = i < parts.length ? (byte) Integer.parseInt(parts[i]) : 0;
        }

        return version;
    }
}

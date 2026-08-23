package com.valiantgaming.launcher.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;

/**
 * Decodes {@code A2L_ansVerifyVersion}. Layout confirmed from the captured example in
 * {@link com.valiantgaming.commons.network.packet.Protocol}'s block comment:
 *
 * <pre>
 * Data: 01 00 09 02 02 04 00 03 00 06 00 00
 *     Launcher Version: 01 00 09 02
 *     Client Version:   02 04 00 03
 *     Unknown:          00 06 00 00
 * </pre>
 *
 * Each version is 4 bytes, one per dotted component (e.g. {@code 01 00 09 02} -> "1.0.9.2",
 * matching {@code AuthServer.ini}'s {@code LAUNCHER_VERSION}). The trailing 4 bytes are
 * unconfirmed - the comment guesses "client protocol", but that doesn't line up with
 * {@code AuthServer.ini}'s {@code CLIENT_PROTOCOL} format, so they're surfaced as raw hex
 * rather than parsed.
 */
public class GetVerifyVersion
{
    public record VerifyVersion(String launcherVersion, String clientVersion, String unknownHex) {}

    public static VerifyVersion decode(byte[] message)
    {
        byte[] payload = Utility.split(message, 2, message.length);

        String launcherVersion = toDottedVersion(Utility.split(payload, 0, 4));
        String clientVersion = toDottedVersion(Utility.split(payload, 4, 8));
        String unknownHex = Utility.byteArrayToHexString(Utility.split(payload, 8, 12));

        return new VerifyVersion(launcherVersion, clientVersion, unknownHex);
    }

    private static String toDottedVersion(byte[] component)
    {
        StringBuilder version = new StringBuilder();

        for(int i = 0; i < component.length; i++)
        {
            if(i > 0)
                version.append('.');

            version.append(component[i] & 0xFF);
        }

        return version.toString();
    }
}

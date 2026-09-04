package com.valiantgaming.authserver.network.packet.client.handler;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.commons.utility.Utility;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;

/**
 * Decodes {@code U2A_askVerify}.
 *
 * <pre>
 * Size      2 bytes  (little-endian, stripped by the frame decoder)
 * Category  1 byte   0x33
 * Protocol  1 byte   0x01
 * Protocol  3 bytes  client protocol, one byte per component - 07 01 01 = "7.1.1"
 * Address   32 bytes null-padded, the login address the client dialled
 * </pre>
 *
 * <p>This matches the EP1 {@code ClientPackets} documentation <b>and</b> the live Classic 2.6.0.1
 * capture in {@code CLIENT-PROTOCOL-NOTES.md} §2 byte for byte - the request layout did not change
 * between the two client generations (§11).
 *
 * <p>What the address field holds depends on {@code LOGIN.INI}'s {@code LOGIN_SERVER_TYPE} (§4),
 * and <b>both forms have been seen on the wire</b>:
 * <ul>
 *   <li>{@code TYPE = 1} - the current configuration - the client sends {@code LOGIN_SERVER_IP}
 *       verbatim, e.g. {@code 127.0.0.1}, which is what {@code [CLIENT] IP} is compared against
 *       below.</li>
 *   <li>{@code TYPE = 2} - the client composes {@code IP_HEAD + <index> + "." + IP_TAIL} and
 *       truncates to 15 characters, so {@code connected1.sunclassic.webzen.co.kr} arrives as
 *       {@code connected1.sunc}. <b>The address check below cannot pass under {@code TYPE = 2}</b>
 *       - switch it off there rather than letting it hard-refuse the client.</li>
 * </ul>
 */
@Log4j2
public class VerifyUser
{
    /** Offset of the 3-byte protocol field within the whole packet (after category + protocol). */
    private static final int PROTOCOL_OFFSET = 2;
    private static final int PROTOCOL_LENGTH = 3;

    /** Offset and width of the null-padded address field within the whole packet. */
    private static final int ADDRESS_OFFSET = PROTOCOL_OFFSET + PROTOCOL_LENGTH;
    private static final int ADDRESS_LENGTH = 32;

    /** Whole-packet length: category + protocol + the two fields above. */
    private static final int PACKET_LENGTH = ADDRESS_OFFSET + ADDRESS_LENGTH;

    private static final String authServerIP = AuthServerConfig.getClientIp();
    private static final String clientProtocol = AuthServerConfig.getClientProtocol();

    /** Everything after category + protocol - the 3-byte protocol and 32-byte address fields. */
    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }

    /**
     * Whether this {@code U2A_askVerify} is accepted. {@code false} makes {@code AnsVerify} send
     * flag {@code 0x01}, which the client treats as a hard refusal, so every rejection is logged
     * with the reason - a silent {@code false} here previously looked exactly like the client
     * ignoring our reply.
     *
     * @param message the whole packet, category byte included
     */
    public static boolean verify(byte[] message)
    {
        log.info("askVerify raw: {}", Utility.byteArrayToHexString(message));

        if(message.length < PACKET_LENGTH)
        {
            log.warn("Rejecting askVerify: expected {} bytes, got {}.", PACKET_LENGTH, message.length);
            return false;
        }

        String userProtocol = readProtocol(message);
        String connectionAddress = readAddress(message);

        log.info("askVerify from a client running protocol {}, dialled '{}'.", userProtocol, connectionAddress);

        if(!userProtocol.equals(clientProtocol))
        {
            log.warn("Rejecting askVerify: client protocol {} does not match [VERSION] CLIENT_PROTOCOL = {}.",
                    userProtocol, clientProtocol);
            return false;
        }

        if(!connectionAddress.equals(authServerIP))
        {
            // Expected whenever LOGIN.INI is on LOGIN_SERVER_TYPE = 2, which sends a truncated
            // hostname rather than an address - see this class's comment.
            log.warn("Rejecting askVerify: login address '{}' does not match [CLIENT] IP = {}.",
                    connectionAddress, authServerIP);
            return false;
        }

        return true;
    }

    /** The 3-byte protocol field as a dotted string, so it compares against the ini value directly. */
    private static String readProtocol(byte[] message)
    {
        StringBuilder version = new StringBuilder();

        for(int i = PROTOCOL_OFFSET; i < ADDRESS_OFFSET; i++)
        {
            if(!version.isEmpty())
                version.append('.');

            version.append(message[i] & 0xFF);
        }

        return version.toString();
    }

    /** The address field with its null padding trimmed. */
    private static String readAddress(byte[] message)
    {
        byte[] address = Utility.cutTail(Utility.split(message, ADDRESS_OFFSET, ADDRESS_OFFSET + ADDRESS_LENGTH));

        return new String(address, StandardCharsets.US_ASCII);
    }
}

package com.valiantgaming.authserver.network.packet.client.handler;

import com.valiantgaming.commons.utility.Utility;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Decodes {@code U2A_askVerify}. The layout below is confirmed against a live client
 * (see {@code CLIENT-PROTOCOL-NOTES.md} §2) - the 35-byte payload is a 3-byte prefix
 * ({@code 07 01 01}) followed by a 32-byte null-padded field carrying the login hostname the
 * client composed from {@code LOGIN.INI}'s {@code SERVICE_LOGIN_SERVER_IP_HEAD}/{@code _IP_TAIL},
 * truncated to 15 characters - e.g. {@code connected1.sunc} for
 * {@code connected1.sunclassic.webzen.co.kr}.
 */
@Log4j2
public class VerifyUser
{
    /** {@code 07 01 01} - fixed prefix ahead of the hostname field. Meaning still unknown. */
    private static final int PREFIX_LENGTH = 3;
    /** Null-padded hostname field; the client truncates the host to 15 chars before padding. */
    private static final int HOST_LENGTH = 32;

    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }

    /**
     * Confirms this is a well-formed {@code U2A_askVerify} and logs the login host the client
     * says it dialled, so a client pointed at the wrong address is visible in the log.
     *
     * <p>This deliberately does <b>not</b> compare the host against config. The hostname lives in
     * {@code LOGIN.INI} inside the client's {@code System.wpk}, which the server has no view of -
     * an earlier version of this method checked the payload for {@code AuthServerConfig}'s client
     * protocol and client IP instead, and always failed, because neither of those appears anywhere
     * in this packet. Rejecting here sends {@code A2U_ansVerify} result {@code 0x01}, which the
     * client treats as a hard refusal, so a check we cannot actually make must not gate it.
     */
    public static boolean verify(byte[] payload)
    {
        log.info("Hex Payload: {}", Utility.byteArrayToHexString(payload));

        if(payload.length < PREFIX_LENGTH + HOST_LENGTH)
        {
            log.warn("Malformed askVerify - expected at least {} bytes, got {}",
                    PREFIX_LENGTH + HOST_LENGTH, payload.length);
            return false;
        }

        byte[] hostBytes = Arrays.copyOfRange(payload, PREFIX_LENGTH, PREFIX_LENGTH + HOST_LENGTH);
        String host = new String(Utility.cutTail(hostBytes), StandardCharsets.US_ASCII);

        log.info("Client login host: '{}'", host);

        if(host.isBlank())
        {
            log.warn("askVerify carried an empty login host");
            return false;
        }

        return true;
    }
}

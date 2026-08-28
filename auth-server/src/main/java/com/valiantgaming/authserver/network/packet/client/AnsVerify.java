package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.authserver.network.packet.client.handler.VerifyUser;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2U_ansVerify}. The request side ({@code U2A_askVerify}) is confirmed against a
 * live client - see {@link VerifyUser} - but <b>this response's format is still unknown</b> and
 * remains the open blocker tracked in {@code CLIENT-PROTOCOL-NOTES.md} §3.
 *
 * <p>Currently mirrors {@code AnsAuthUser}'s single result-byte convention ({@code 0x00} success,
 * {@code 0x01} failure). A live client rejects the 3-byte {@code 33 02 00} form, so the real
 * reply is probably longer; candidates are listed in §3. Note the result byte still matters even
 * while the shape is wrong - sending {@code 0x01} is a hard refusal, so {@link VerifyUser#verify}
 * must only fail on a genuinely malformed request.
 */
public class AnsVerify
{
    public static byte[] createPacket(byte[] payload)
    {
        byte message = VerifyUser.verify(payload) ? (byte) 0x00 : (byte) 0x01;

        return new byte[] { Category.AUTH, Protocol.A2U_ansVerify, message };
    }
}

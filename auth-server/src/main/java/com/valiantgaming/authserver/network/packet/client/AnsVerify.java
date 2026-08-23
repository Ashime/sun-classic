package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2U_ansVerify}. No example of {@code U2A_askVerify}/{@code A2U_ansVerify}
 * has been captured, so the payload semantics are unconfirmed - this is a placeholder
 * success acknowledgement (mirrors {@code AnsAuthUser}'s single result-byte convention)
 * until real capture data narrows down what "verify" actually checks.
 */
public class AnsVerify
{
    public byte[] createPacket()
    {
        return new byte[] { Category.AUTH, Protocol.A2U_ansVerify, 0x00 };
    }
}

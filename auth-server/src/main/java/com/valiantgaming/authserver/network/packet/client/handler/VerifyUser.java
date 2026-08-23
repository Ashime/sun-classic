package com.valiantgaming.authserver.network.packet.client.handler;

import com.valiantgaming.commons.utility.Utility;

/**
 * Decodes {@code U2A_askVerify}. No example of this packet has been captured, so the payload
 * is returned as-is (whatever follows category+protocol) for logging rather than parsed into
 * named fields - see {@code AnsVerify}'s class comment.
 */
public class VerifyUser
{
    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }
}

package com.valiantgaming.authserver.network.packet.client.handler;

import com.valiantgaming.commons.utility.Utility;

/**
 * Decodes {@code U2A_askSrvSelect}. No example of this packet has been captured - presumably
 * it carries the chosen server/channel IDs from {@code A2U_ansSrvList_Srv}/{@code _Chn}, but
 * until that's confirmed the payload is just returned as raw bytes - see
 * {@code AnsSrvSelect}'s class comment.
 */
public class SrvSelect
{
    public static byte[] decode(byte[] message)
    {
        return Utility.split(message, 2, message.length);
    }
}

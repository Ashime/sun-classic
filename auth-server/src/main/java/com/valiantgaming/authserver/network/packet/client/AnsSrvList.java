package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;

/**
 * Builds {@code A2U_ansSrvList_Srv} (server list) and {@code A2U_ansSrvList_Chn} (channel
 * list for a selected server), sent in response to {@code U2A_askSrvList}.
 *
 * <p>Neither packet's payload layout has been captured, and there's no S2S query yet for
 * auth-server to ask database-server for the live {@code ServerInfo} rows it would need to
 * populate a real list (see {@code DatabaseManager}, which already loads them at startup on
 * the database-server side). Both methods below are placeholders that report zero entries -
 * revisit once both the wire format and that S2S query exist.
 */
public class AnsSrvList
{
    public byte[] createServerListPacket()
    {
        // TODO: replace with real ServerInfo entries once an S2S "list servers" query exists.
        return new byte[] { Category.AUTH, Protocol.A2U_ansSrvList_Srv, 0x00 };
    }

    public byte[] createChannelListPacket()
    {
        // TODO: replace with real channel/sub-server entries once the wire format is known.
        return new byte[] { Category.AUTH, Protocol.A2U_ansSrvList_Chn, 0x00 };
    }
}

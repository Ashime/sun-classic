package com.valiantgaming.authserver.network.packet.client;

import com.valiantgaming.authserver.database.entity.server.ServerInfo;
import com.valiantgaming.authserver.network.session.server.GameServerRegistry;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds {@code A2U_ansSrvList_Srv} (the server list) and {@code A2U_ansSrvList_Chn} (the channel
 * list), sent in response to {@code U2A_askSrvList}.
 *
 * <p>Layouts come from the EP1 {@code ServerPackets} documentation, transcribed in
 * {@code CLIENT-PROTOCOL-NOTES.md} §11.4. The 2-byte little-endian length header is added by
 * {@code ClientPacketEncoder}, so everything below starts at the category byte:
 *
 * <pre>
 * Servers  0x33 | 0x11 | count(1) | entry [ 0x00 entry ]*
 *          entry = Name(32, null-padded) | Unknown(1) | Server#(1) | Unknown(1)
 *
 * Channels 0x33 | 0x12 | count(1) | entry [ 0x00 entry ]*
 *          entry = Name(33, null-padded) | Server#(1) | Channel#(1) | Terminator(1, NOT 0x00)
 * </pre>
 *
 * <p>Note the asymmetry, which is easy to get wrong: server names are <b>32</b> bytes, channel
 * names are <b>33</b>. The single {@code 0x00} separator sits <i>between</i> entries only - there
 * is none after the last one.
 *
 * <p><b>These layouts are EP1-era and unverified against the Classic client.</b> §11 found the
 * request side unchanged between generations and {@code U2A_askSrvList} confirmed exactly
 * ({@code 33 0F}, no body - §13.3), but {@code askAuthUser} did change, so a response layout
 * surviving is not guaranteed. The three unknown/terminator bytes are the likeliest thing to be
 * wrong - see {@link #SERVER_NAME_TERMINATOR} and {@link #SERVER_ENTRY_TERMINATOR}.
 *
 * <p><b>Data source, and it is the wrong one.</b> Servers come from {@link GameServerRegistry},
 * which holds the one {@code ServerInfo} row the S2S handshake already fetches - but
 * {@code ServerInfo} is the infrastructure registry (AUTH/GAME/DB/WEB and their S2S ports), not
 * player-facing worlds. Channels are worse: nothing queries them, so one is synthesised per server
 * (see {@link #CHANNEL_NAME}). Both were enough to prove the layouts against a real client (§16)
 * and neither is right.
 *
 * <p>The real sources are the {@code GameServerInfo} and {@code ChannelInfo} tables, which exist
 * and are empty, and which need stored procedures, DAOs and an S2S list query before this can read
 * them - see {@code CLIENT-PROTOCOL-NOTES.md} §18 for the full handoff.
 */
@Log4j2
public class AnsSrvList
{
    /** Fixed width of a server name, null-padded. Channel names are one byte wider - see below. */
    private static final int SERVER_NAME_LENGTH = 32;

    /** Fixed width of a channel name, null-padded. Deliberately 33, not 32 - see the class comment. */
    private static final int CHANNEL_NAME_LENGTH = 33;

    /** Single {@code 0x00} written between entries, never after the last. */
    private static final byte ENTRY_SEPARATOR = 0x00;

    /**
     * The documentation's first "Unknown (1 byte)" in a server entry, sitting between the name and
     * the server number. Written as {@code 0x00}, which is what the documentation implies and also
     * the reading that makes the server name field effectively 33 bytes like the channel one.
     */
    private static final byte SERVER_NAME_TERMINATOR = 0x00;

    /**
     * The documentation's second "Unknown (1 byte)", closing a server entry.
     *
     * <p><b>Prime suspect if the client rejects the server list.</b> The documentation gives no
     * value for it, but the channel entry has a byte in the same position that it explicitly says
     * "cannot be 0x00". If these two fields are the same thing, this should be
     * {@link #CHANNEL_TERMINATOR} instead. Left at the literal documented reading until a client
     * says otherwise; flipping it is a one-line change.
     */
    private static final byte SERVER_ENTRY_TERMINATOR = 0x00;

    /** Closes a channel entry. The documentation is explicit that this must not be {@code 0x00}. */
    private static final byte CHANNEL_TERMINATOR = 0x01;

    /**
     * Placeholder channel name. Nothing in this stack models channels - there is no table, no
     * config and no S2S query for them - so one channel is synthesised per server purely so the
     * client has something selectable and can be driven on to {@code U2A_askSrvSelect}.
     */
    private static final String CHANNEL_NAME = "Channel 1";

    /**
     * Numbering handed to the client for both servers and channels. Zero-based on the assumption
     * that these are indexes rather than display numbers - <b>unconfirmed</b>. The client echoes
     * both back in {@code U2A_askSrvSelect} (see {@code SrvSelect}, which logs the payload), so the
     * first selection a client makes settles the convention.
     */
    private static final int FIRST_NUMBER = 0;

    public byte[] createServerListPacket()
    {
        List<ServerInfo> servers = GameServerRegistry.getServers();

        if(servers.isEmpty())
            log.warn("Answering askSrvList with an empty server list - no ServerInfo from database-server yet. " +
                    "Start database-server before auth-server; the S2S connection is never retried.");

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(servers.size());

        for(int index = 0; index < servers.size(); index++)
        {
            if(index > 0)
                body.write(ENTRY_SEPARATOR);

            writeFixed(body, servers.get(index).getServerName(), SERVER_NAME_LENGTH);
            body.write(SERVER_NAME_TERMINATOR);
            body.write(FIRST_NUMBER + index);
            body.write(SERVER_ENTRY_TERMINATOR);
        }

        return packet(Protocol.A2U_ansSrvList_Srv, body.toByteArray());
    }

    /**
     * One synthesised channel per server, so a client that picks a server has something to pick
     * inside it. See {@link #CHANNEL_NAME} for why this is not real data.
     */
    public byte[] createChannelListPacket()
    {
        List<ServerInfo> servers = GameServerRegistry.getServers();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(servers.size());

        for(int index = 0; index < servers.size(); index++)
        {
            if(index > 0)
                body.write(ENTRY_SEPARATOR);

            writeFixed(body, CHANNEL_NAME, CHANNEL_NAME_LENGTH);
            body.write(FIRST_NUMBER + index);
            body.write(FIRST_NUMBER);
            body.write(CHANNEL_TERMINATOR);
        }

        return packet(Protocol.A2U_ansSrvList_Chn, body.toByteArray());
    }

    /**
     * Writes {@code value} as exactly {@code length} bytes, null-padded, truncating anything
     * longer. Truncation is logged rather than silent: a name clipped mid-way is the sort of thing
     * that shows up as an unreadable entry on the client's screen and is otherwise hard to trace.
     */
    private static void writeFixed(ByteArrayOutputStream out, String value, int length)
    {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.US_ASCII);

        if(bytes.length > length)
            log.warn("Name '{}' is {} bytes and does not fit the {}-byte field - truncating.", value, bytes.length, length);

        out.write(bytes, 0, Math.min(bytes.length, length));

        for(int i = bytes.length; i < length; i++)
            out.write(0x00);
    }

    private static byte[] packet(byte protocol, byte[] body)
    {
        byte[] packet = new byte[2 + body.length];
        packet[0] = Category.AUTH;
        packet[1] = protocol;
        System.arraycopy(body, 0, packet, 2, body.length);

        return packet;
    }
}

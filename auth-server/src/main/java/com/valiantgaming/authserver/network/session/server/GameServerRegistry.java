package com.valiantgaming.authserver.network.session.server;

import com.valiantgaming.authserver.database.entity.server.ServerInfo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The game servers auth-server can offer a client in {@code A2U_ansSrvList_Srv}.
 *
 * <p>Today that is exactly one row, and it arrives for free: the S2S handshake already asks
 * database-server for {@code "GAME SERVER"} ({@code AskServerInfo} -> {@code PacketHandler}'s
 * {@code S2S_askServerInfo} branch) and gets back a full {@code ServerInfo} with its address and
 * port. {@code ServerPacketHandler} used to decode that, log it and drop it on the floor; it is
 * kept here instead so {@code AnsSrvList} has real data to answer with rather than a placeholder.
 *
 * <p><b>This is a stopgap on the wrong table.</b> {@code ServerInfo} is this stack's own
 * infrastructure registry - its four rows are DATABASE/AUTH/GAME/WEB SERVER and their <i>S2S</i>
 * addresses, not player-facing worlds. It was used because it is the only live data auth-server
 * has, and it was enough to prove the {@code ansSrvList} layouts against a real client (§16).
 *
 * <p>The right sources are the {@code GameServerInfo} and {@code ChannelInfo} tables, which already
 * exist and are empty - see {@code CLIENT-PROTOCOL-NOTES.md} §18. The distinction bites at
 * {@code A2U_ansSrvSelect}: the port in this row is the game server's S2S listener, and handing it
 * to a client would point it at the wrong socket.
 *
 * <p>Empty until the S2S handshake completes, which is normal at startup and for as long as
 * database-server is unreachable - {@code NioServer#initS2S} connects once and never retries, so
 * an auth-server started first stays empty until it is restarted.
 */
public final class GameServerRegistry
{
    private static final AtomicReference<ServerInfo> gameServer = new AtomicReference<>();

    private GameServerRegistry()
    {
    }

    public static void setGameServer(ServerInfo serverInfo)
    {
        gameServer.set(serverInfo);
    }

    public static Optional<ServerInfo> getGameServer()
    {
        return Optional.ofNullable(gameServer.get());
    }

    /**
     * Every server that can be offered to a client, in the order the client will index them.
     * Empty rather than null when nothing is known yet, so callers can build an empty list packet.
     */
    public static List<ServerInfo> getServers()
    {
        ServerInfo serverInfo = gameServer.get();

        return serverInfo == null ? List.of() : List.of(serverInfo);
    }
}

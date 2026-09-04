package com.valiantgaming.authserver.server.handler;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.authserver.network.packet.client.*;
import com.valiantgaming.authserver.network.packet.client.handler.AuthUser;
import com.valiantgaming.authserver.network.packet.client.handler.SrvSelect;
import com.valiantgaming.authserver.network.packet.client.handler.VerifyUser;
import com.valiantgaming.authserver.network.packet.client.launcher.AnsLauncherReady;
import com.valiantgaming.authserver.network.packet.client.launcher.AnsVerifyVersion;
import com.valiantgaming.authserver.network.packet.server.AskAuthUser;
import com.valiantgaming.authserver.network.session.client.ClientSession;
import com.valiantgaming.authserver.network.session.client.ClientSessionManager;
import com.valiantgaming.authserver.network.session.server.PendingAuthRequests;
import com.valiantgaming.authserver.network.session.server.ServerSessionManager;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dispatches both flows documented in {@code Protocol} that share this listener: the game
 * client login flow ("SERVER TO CLIENT" block - sends {@code A2U_ansReady} as soon as a
 * client connects, then answers {@code U2A_askVerify}, {@code U2A_askAuthUser},
 * {@code U2A_askSrvList}, and {@code U2A_askSrvSelect} in turn), and the launcher's
 * handshake ({@code L2A_askUnknown1} -> {@code A2L_ansReady} + {@code A2L_ansVerifyVersion}).
 * Mirrors {@code ServerPacketHandler}'s S2S dispatch shape.
 *
 * <p>The launcher and game client connect to the same port/category with no way to tell them
 * apart before either side speaks (see {@code Protocol}'s capture: both flows use
 * {@code Category.AUTH}), and the documented game-client flow has the server speak first
 * ({@code A2U_ansReady}) while the documented launcher flow has the client speak first
 * ({@code L2A_askUnknown1}). Rather than guess which one a fresh connection is,
 * {@link #channelActive} keeps sending {@code A2U_ansReady} unconditionally like before, and
 * {@link #channelRead} separately reacts if a {@code L2A_askUnknown1} arrives. A real
 * launcher connection will therefore also receive one unsolicited, unrecognized
 * {@code A2U_ansReady} it logs and ignores - a known rough edge until a real capture settles
 * how these connections are actually meant to be told apart.
 *
 * <p>Several of the game-client packets' real payload layouts are unconfirmed and their
 * answers are placeholders - see the class comments on {@code AnsVerify}/{@code AnsSrvList}/
 * {@code AnsSrvSelect} and on {@code AuthUser} for exactly what's still stubbed.
 *
 * <h2>Reading the log</h2>
 * Every line carries a {@code [conn-N]} tag, and connections that never send a byte are logged
 * at DEBUG rather than INFO. Both exist because this listener is not only spoken to by the game
 * client: the launcher opens a persistent connection at startup ({@code NioClient}) <i>and</i>
 * re-connects every five seconds as a reachability probe
 * ({@code ServerHealthCheck#isReachable}), so at INFO the log used to be a stream of
 * connect/disconnect pairs no different in shape from a real client's.
 *
 * <p>A heartbeat probe is now a DEBUG connect and a DEBUG close with {@code 0 packet(s)}; a real
 * game client is the only thing that produces INFO {@code [conn-N] <- } lines. So "did the client
 * answer our {@code A2U_ansVerify}?" is answered by whether {@code conn-N} logs a second inbound
 * packet, and the close line for that same {@code conn-N} states the total either way.
 */
@Log4j2
public class ClientPacketHandler extends ChannelDuplexHandler
{
    /** Per-JVM connection counter, so every log line can name which connection it belongs to. */
    private static final AtomicLong CONNECTION_COUNTER = new AtomicLong();

    /**
     * How many times one connection may be sent {@code AnsVerifyProbe}'s re-trigger. Diagnostic
     * only, and only a runaway guard: the client re-verifies on every re-trigger, so without a
     * bound the two would ping-pong forever, spin the log and wrap the candidate rotation. Sized to
     * comfortably clear the 32-entry opcode sweep in one launch.
     */
    private static final int MAX_PROBE_RETRIGGERS = 40;

    // One handler instance per channel (NioServer#initC2S news one up in initChannel), so these
    // are per-connection state, not shared.
    private final long connectionId = CONNECTION_COUNTER.incrementAndGet();
    private long connectedAt;
    private int packetsReceived;
    private int probeRetriggersSent;

    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        connectedAt = System.nanoTime();

        // DEBUG, not INFO: most connections here are the launcher's five-second reachability
        // probe, which connects and closes without speaking. channelRead announces anything that
        // actually says something.
        log.debug("[conn-{}] Client connected from {}", connectionId, ctx.channel().remoteAddress());

        ClientSessionManager.getInstance().addSession(ctx);
        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        ctx.writeAndFlush(new AnsReady().createPacket(session));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;

        if(++packetsReceived == 1)
            log.info("[conn-{}] First inbound packet from {} - this connection is a real client, not a health-check probe.",
                    connectionId, ctx.channel().remoteAddress());

        log.info("[conn-{}] <- #{} {}", connectionId, packetsReceived, Utility.byteArrayToHexString(message));

        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        if(message[0] == Category.AUTH)
        {
            switch(message[1])
            {
                case Protocol.L2A_askUnknown1:
                {
                    log.info("[conn-{}] Launcher handshake started by {}", connectionId, ctx.channel().remoteAddress());

                    ctx.writeAndFlush(new AnsLauncherReady().createPacket());
                    ctx.writeAndFlush(new AnsVerifyVersion().createPacket());
                    break;
                }
                case Protocol.U2A_askVerify:
                {
                    byte[] payload = VerifyUser.decode(message);
                    log.info("[conn-{}] Received verify request, payload: {}", connectionId, Utility.byteArrayToHexString(payload));

                    if(AuthServerConfig.isAnsVerifyProbe())
                    {
                        // Diagnostic mode: serve a different candidate per connection to find the
                        // real response format. See AnsVerifyProbe.
                        AnsVerifyProbe.Attempt attempt = AnsVerifyProbe.next(payload);
                        log.info("[conn-{}] PROBE serving ansVerify candidate {} -> {}", connectionId, attempt.label(), attempt.hex());

                        session.setAnsVerifyCandidate(attempt.label());
                        session.setAnsVerifySentAt(System.nanoTime());

                        // Flushed one at a time rather than written and flushed once: the opcode
                        // burst candidates send eight replies here, and with TCP_NODELAY each flush
                        // is its own segment. Coalescing them into one would make a burst also a
                        // test of whether the client reassembles several frames from a single read,
                        // which is not what is being measured.
                        for(byte[] packet : attempt.packets())
                            ctx.writeAndFlush(packet);

                        // Then nudge the client into asking again, so the next candidate can be
                        // served on this same connection instead of costing another client launch
                        // (see AnsVerifyProbe#RETRIGGER). Budgeted because the client answers every
                        // re-trigger with a fresh askVerify - left unbounded the two would loop.
                        if(attempt.retrigger() && probeRetriggersSent < MAX_PROBE_RETRIGGERS)
                        {
                            probeRetriggersSent++;
                            ctx.writeAndFlush(AnsVerifyProbe.retriggerPacket());
                        }
                        else if(attempt.retrigger())
                        {
                            log.warn("[conn-{}] PROBE re-trigger budget of {} exhausted - stopping here. " +
                                            "Relaunch the client to continue the rotation.",
                                    connectionId, MAX_PROBE_RETRIGGERS);
                        }
                    }
                    else
                    {
                        ctx.writeAndFlush(AnsVerify.createPacket(message));
                    }
                    break;
                }
                case Protocol.U2A_askAuthUser:
                {
                    AuthUser.Credentials credentials = AuthUser.decode(message, session.getTeaKey());
                    Channel dbChannel = ServerSessionManager.getChannel();

                    if(dbChannel == null || !dbChannel.isActive())
                    {
                        log.error("[conn-{}] No active connection to Database Server - cannot authenticate {}", connectionId, credentials.username());
                        ctx.writeAndFlush(AnsAuthUser.createPacket(false));
                        break;
                    }

                    // The actual accept/reject decision comes back later on the S2S channel (S2S_ansAuthUser) -
                    // see ServerPacketHandler, which uses this request ID to find its way back to this client.
                    int requestId = PendingAuthRequests.register(ctx, credentials.username());
                    dbChannel.writeAndFlush(new AskAuthUser().createPacket(requestId, credentials.username(), credentials.password()));
                    break;
                }
                case Protocol.U2A_askSrvList:
                {
                    if(!isAllowed(session, ctx, "the server list"))
                        return;

                    AnsSrvList ansSrvList = new AnsSrvList();
                    ctx.writeAndFlush(ansSrvList.createServerListPacket());
                    ctx.writeAndFlush(ansSrvList.createChannelListPacket());
                    break;
                }
                case Protocol.U2A_askSrvSelect:
                {
                    if(!isAllowed(session, ctx, "select a server"))
                        return;

                    byte[] payload = SrvSelect.decode(message);
                    log.info("[conn-{}] Received server select request, payload: {}", connectionId, Utility.byteArrayToHexString(payload));

                    ctx.writeAndFlush(new AnsSrvSelect().createPacket());
                    break;
                }
                default:
                {
                    log.warn("[conn-{}] Unknown packet! Packet: {}", connectionId, Utility.byteArrayToHexString(message));
                }
            }
        }

//        if(message[1] == Protocol.L2A_askUnknown1)
//        {
//            log.info("Launcher handshake started by " + ctx.channel().remoteAddress());
//
//            ctx.writeAndFlush(new AnsLauncherReady().createPacket());
//            ctx.writeAndFlush(new AnsVerifyVersion().createPacket());
//        }
//        else if(message[1] == Protocol.U2A_askVerify)
//        {
//            byte[] payload = VerifyUser.decode(message);
//            log.info("Received verify request, payload: " + Utility.byteArrayToHexString(payload));
//
//            ctx.writeAndFlush(new AnsVerify().createPacket());
//        }
//        else if(message[1] == Protocol.U2A_askAuthUser)
//        {
//            String ipAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
//            byte[] response = AnsAuthUser.createPacket(message, session.getTeaKey(), ipAddress);
//
//            // AnsAuthUser replies 0x00 on success - see its class comment for the byte convention.
//            session.setAuthenticated(response[2] == 0x00);
//
//            ctx.writeAndFlush(response);
//        }
//        else if(message[1] == Protocol.U2A_askSrvList)
//        {
//            if(!session.isAuthenticated())
//            {
//                log.warn("Client " + ctx.channel().remoteAddress() + " asked for the server list before authenticating - dropping.");
//                return;
//            }
//
//            AnsSrvList ansSrvList = new AnsSrvList();
//            ctx.writeAndFlush(ansSrvList.createServerListPacket());
//            ctx.writeAndFlush(ansSrvList.createChannelListPacket());
//        }
//        else if(message[1] == Protocol.U2A_askSrvSelect)
//        {
//            if(!session.isAuthenticated())
//            {
//                log.warn("Client " + ctx.channel().remoteAddress() + " asked to select a server before authenticating - dropping.");
//                return;
//            }
//
//            byte[] payload = SrvSelect.decode(message);
//            log.info("Received server select request, payload: " + Utility.byteArrayToHexString(payload));
//
//            ctx.writeAndFlush(new AnsSrvSelect().createPacket());
//        }
//        else
//        {
//            log.warn("Unknown packet! Packet: {}", Utility.byteArrayToHexString(message));
//        }
    }

    /**
     * Whether a post-login packet should be served. Normally that means the session authenticated;
     * <b>while the ansVerify probe is on, it does not</b>.
     *
     * <p>The probe reaches these packets by sending {@code 33 0E 00} ({@code A2U_ansAuthUser},
     * success) directly, which the client acts on without ever sending {@code U2A_askAuthUser} - so
     * the session never learns it is authenticated and the gate rejected exactly the traffic the
     * probe exists to produce (see {@code CLIENT-PROTOCOL-NOTES.md} §14.2 and §14.3).
     *
     * <p>Tied to the probe flag rather than a new setting of its own because the two are inseparable
     * in practice: without the probe the client never gets past verify, so it never reaches here
     * unauthenticated anyway. That also means this relaxation cannot be left on by accident in
     * production - {@code ANS_VERIFY_PROBE} is off there, and it already announces itself loudly.
     */
    private boolean isAllowed(ClientSession session, ChannelHandlerContext ctx, String what)
    {
        if(session.isAuthenticated())
            return true;

        if(AuthServerConfig.isAnsVerifyProbe())
        {
            log.warn("[conn-{}] PROBE: client {} asked for {} without authenticating - answering anyway.",
                    connectionId, ctx.channel().remoteAddress(), what);
            return true;
        }

        log.warn("[conn-{}] Client {} asked for {} before authenticating - dropping.",
                connectionId, ctx.channel().remoteAddress(), what);

        return false;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        long openMillis = (System.nanoTime() - connectedAt) / 1_000_000L;

        if(packetsReceived == 0)
        {
            // Silent connection: the launcher's reachability probe, or a connection closed before
            // it could speak (e.g. UniqueIpFilter rejecting it - see NioServer#initC2S). Neither is
            // worth an INFO line, and at INFO they used to drown out real client traffic.
            log.debug("[conn-{}] {} closed after {}ms without sending anything.",
                    connectionId, ctx.channel().remoteAddress(), openMillis);
        }
        else
        {
            log.info("[conn-{}] {} disconnected after {}ms having sent {} packet(s).",
                    connectionId, ctx.channel().remoteAddress(), openMillis, packetsReceived);
        }

        // Read the session before removing it - how long the client tolerated our ansVerify is
        // the probe's actual measurement, and it is only knowable at close time.
        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        if(session != null && session.getAnsVerifyCandidate() != null)
        {
            long elapsedMillis = (System.nanoTime() - session.getAnsVerifySentAt()) / 1_000_000L;
            log.info("[conn-{}] PROBE result: candidate {} -> disconnected after {}ms, {} packet(s) received in total.",
                    connectionId, session.getAnsVerifyCandidate(), elapsedMillis, packetsReceived);
        }

        ClientSessionManager.getInstance().removeSession(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        if(cause instanceof IOException)
        {
            // Expected: the client's process died, the network dropped, or (most commonly) a
            // health-check probe closed the socket without reading what we'd just written
            // (see ServerHealthCheck.isReachable) - not an application bug, so DEBUG rather than WARN.
            log.debug("[conn-{}] Connection to {} was reset: {}", connectionId, ctx.channel().remoteAddress(), cause.getMessage());
        }
        else
        {
            log.error("[conn-{}] Unexpected error on channel {}", connectionId, ctx.channel().remoteAddress(), cause);
        }

        ctx.close();
    }
}

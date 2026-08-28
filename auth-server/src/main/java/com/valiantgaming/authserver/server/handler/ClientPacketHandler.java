package com.valiantgaming.authserver.server.handler;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.authserver.network.packet.client.*;
import com.valiantgaming.authserver.network.packet.client.handler.AuthUser;
import com.valiantgaming.authserver.network.packet.client.handler.SrvSelect;
import com.valiantgaming.authserver.network.packet.client.handler.VerifyUser;
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
 */
@Log4j2
public class ClientPacketHandler extends ChannelDuplexHandler
{
    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        log.info("Client connected from " + ctx.channel().remoteAddress());

        ClientSessionManager.getInstance().addSession(ctx);
        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        ctx.writeAndFlush(new AnsReady().createPacket(session));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;
        log.info("Message: {}", Utility.byteArrayToHexString(message));

        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        if(message[0] == Category.AUTH)
        {
            switch(message[1])
            {
                case Protocol.L2A_askUnknown1:
                {
                    log.info("Launcher handshake started by {}", ctx.channel().remoteAddress());

                    ctx.writeAndFlush(new AnsLauncherReady().createPacket());
                    ctx.writeAndFlush(new AnsVerifyVersion().createPacket());
                    break;
                }
                case Protocol.U2A_askVerify:
                {
                    byte[] payload = VerifyUser.decode(message);
                    log.info("Received verify request, payload: {}", Utility.byteArrayToHexString(payload));

                    if(AuthServerConfig.isAnsVerifyProbe())
                    {
                        // Diagnostic mode: serve a different candidate per connection to find the
                        // real response format. See AnsVerifyProbe.
                        AnsVerifyProbe.Attempt attempt = AnsVerifyProbe.next(payload);
                        log.info("PROBE serving ansVerify candidate {} -> {}", attempt.label(), attempt.hex());

                        session.setAnsVerifyCandidate(attempt.label());
                        session.setAnsVerifySentAt(System.nanoTime());

                        ctx.writeAndFlush(attempt.packet());
                    }
                    else
                    {
                        ctx.writeAndFlush(AnsVerify.createPacket(payload));
                    }
                    break;
                }
                case Protocol.U2A_askAuthUser:
                {
                    AuthUser.Credentials credentials = AuthUser.decode(message, session.getTeaKey());
                    Channel dbChannel = ServerSessionManager.getChannel();

                    if(dbChannel == null || !dbChannel.isActive())
                    {
                        log.error("No active connection to Database Server - cannot authenticate {}", credentials.username());
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
                    if(!session.isAuthenticated())
                    {
                        log.warn("Client {} asked for the server list before authenticating - dropping.", ctx.channel().remoteAddress());
                        return;
                    }

                    AnsSrvList ansSrvList = new AnsSrvList();
                    ctx.writeAndFlush(ansSrvList.createServerListPacket());
                    ctx.writeAndFlush(ansSrvList.createChannelListPacket());
                    break;
                }
                case Protocol.U2A_askSrvSelect:
                {
                    if(!session.isAuthenticated())
                    {
                        log.warn("Client {} asked to select a server before authenticating - dropping.", ctx.channel().remoteAddress());
                        return;
                    }

                    byte[] payload = SrvSelect.decode(message);
                    log.info("Received server select request, payload: {}", Utility.byteArrayToHexString(payload));

                    ctx.writeAndFlush(new AnsSrvSelect().createPacket());
                    break;
                }
                default:
                {
                    log.warn("Unknown packet! Packet: {}", Utility.byteArrayToHexString(message));
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

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        log.info("Client at {} disconnected.", ctx.channel().remoteAddress());

        // Read the session before removing it - how long the client tolerated our ansVerify is
        // the probe's actual measurement, and it is only knowable at close time.
        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        if(session != null && session.getAnsVerifyCandidate() != null)
        {
            long elapsedMillis = (System.nanoTime() - session.getAnsVerifySentAt()) / 1_000_000L;
            log.info("PROBE result: candidate {} -> disconnected after {}ms", session.getAnsVerifyCandidate(), elapsedMillis);
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
            log.debug("Connection to {} was reset: {}", ctx.channel().remoteAddress(), cause.getMessage());
        }
        else
        {
            log.error("Unexpected error on channel {}", ctx.channel().remoteAddress(), cause);
        }

        ctx.close();
    }
}

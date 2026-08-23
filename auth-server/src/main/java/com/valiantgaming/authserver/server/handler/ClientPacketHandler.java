package com.valiantgaming.authserver.server.handler;

import com.valiantgaming.authserver.network.packet.client.*;
import com.valiantgaming.authserver.network.packet.client.handler.SrvSelect;
import com.valiantgaming.authserver.network.packet.client.handler.VerifyUser;
import com.valiantgaming.authserver.network.session.client.ClientSession;
import com.valiantgaming.authserver.network.session.client.ClientSessionManager;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Dispatches the game client login flow documented in {@code Protocol}'s "SERVER TO CLIENT"
 * block: sends {@code A2U_ansReady} as soon as a client connects, then answers
 * {@code U2A_askVerify}, {@code U2A_askAuthUser}, {@code U2A_askSrvList}, and
 * {@code U2A_askSrvSelect} in turn. Mirrors {@code ServerPacketHandler}'s S2S dispatch shape.
 *
 * <p>Several of these packets' real payload layouts are unconfirmed and their answers are
 * placeholders - see the class comments on {@code AnsVerify}/{@code AnsSrvList}/
 * {@code AnsSrvSelect} and on {@code AuthUser} for exactly what's still stubbed.
 */
@Log4j2
public class ClientPacketHandler extends ChannelDuplexHandler
{
    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        log.info("Game client connected from " + ctx.channel().remoteAddress());

        ClientSessionManager.getInstance().addSession(ctx);
        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        ctx.writeAndFlush(new AnsReady().createPacket(session));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;
        log.info("Message: " + Utility.byteArrayToHexString(message));

        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        if(message[1] == Protocol.U2A_askVerify)
        {
            byte[] payload = VerifyUser.decode(message);
            log.info("Received verify request, payload: " + Utility.byteArrayToHexString(payload));

            ctx.writeAndFlush(new AnsVerify().createPacket());
        }
        else if(message[1] == Protocol.U2A_askAuthUser)
        {
            String ipAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            byte[] response = AnsAuthUser.createPacket(message, session.getTeaKey(), ipAddress);

            // AnsAuthUser replies 0x00 on success - see its class comment for the byte convention.
            session.setAuthenticated(response[2] == 0x00);

            ctx.writeAndFlush(response);
        }
        else if(message[1] == Protocol.U2A_askSrvList)
        {
            if(!session.isAuthenticated())
            {
                log.warn("Client " + ctx.channel().remoteAddress() + " asked for the server list before authenticating - dropping.");
                return;
            }

            AnsSrvList ansSrvList = new AnsSrvList();
            ctx.writeAndFlush(ansSrvList.createServerListPacket());
            ctx.writeAndFlush(ansSrvList.createChannelListPacket());
        }
        else if(message[1] == Protocol.U2A_askSrvSelect)
        {
            if(!session.isAuthenticated())
            {
                log.warn("Client " + ctx.channel().remoteAddress() + " asked to select a server before authenticating - dropping.");
                return;
            }

            byte[] payload = SrvSelect.decode(message);
            log.info("Received server select request, payload: " + Utility.byteArrayToHexString(payload));

            ctx.writeAndFlush(new AnsSrvSelect().createPacket());
        }
        else
        {
            log.warn("Unknown packet! Packet: {}", Utility.byteArrayToHexString(message));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        log.info("Game client at " + ctx.channel().remoteAddress() + " disconnected.");
        ClientSessionManager.getInstance().removeSession(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        if(cause instanceof IOException)
        {
            // Expected: the client's process died or the network dropped - not an application bug.
            log.warn("Connection to " + ctx.channel().remoteAddress() + " was reset: " + cause.getMessage());
        }
        else
        {
            log.error("Unexpected error on channel " + ctx.channel().remoteAddress(), cause);
        }

        ctx.close();
    }
}

package com.valiantgaming.authserver.server.handler;

import com.valiantgaming.authserver.database.entity.server.ServerInfo;
import com.valiantgaming.authserver.network.packet.client.AnsAuthUser;
import com.valiantgaming.authserver.network.packet.server.AskServerInfo;
import com.valiantgaming.authserver.network.packet.server.handler.GetAuthUser;
import com.valiantgaming.authserver.network.packet.server.handler.GetServerInfo;
import com.valiantgaming.authserver.network.session.client.ClientSession;
import com.valiantgaming.authserver.network.session.client.ClientSessionManager;
import com.valiantgaming.authserver.network.session.server.PendingAuthRequests;
import com.valiantgaming.authserver.network.session.server.ServerSession;
import com.valiantgaming.authserver.network.session.server.ServerSessionManager;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class ServerPacketHandler extends ChannelDuplexHandler
{

    @Override
    @SneakyThrows
    public void channelActive(ChannelHandlerContext ctx)
    {
        // Wait for the mutual TLS handshake to finish before sending anything application-level: the SslHandler
        // added ahead of this handler is what now proves and encrypts this connection, replacing the old
        // AskAesFileKey/AskRsaKey/AskAesKey packet exchange.
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(future ->
        {
            if(future.isSuccess())
            {
                log.info("TLS handshake completed with " + ctx.channel().remoteAddress());
                ctx.writeAndFlush(new AskServerInfo().createPacket());
            }
            else
            {
                log.error("TLS handshake failed with " + ctx.channel().remoteAddress(), future.cause());
                ctx.close();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        // Fires on ANY closure of this channel - whether the Database Server closed it cleanly or the
        // connection was reset - so this is the one reliable place to clean up session state.
        log.info("Connection to " + ctx.channel().remoteAddress() + " was closed.");

        ServerSession session = ServerSessionManager.getInstance().getSession(ctx.channel().remoteAddress());
        if(session != null)
        {
            ServerSessionManager.getInstance().removeSession(session);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        if(cause instanceof IOException)
        {
            // Expected: the peer's process died or the network dropped - not an application bug.
            log.warn("Connection to " + ctx.channel().remoteAddress() + " was reset: " + cause.getMessage());
        }
        else
        {
            log.error("Unexpected error on channel " + ctx.channel().remoteAddress(), cause);
        }

        ctx.close();
    }

    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;
        log.info("Message: " + Utility.byteArrayToHexString(message));

        if(message[1] == Protocol.S2S_ansServerInfo)
        {
            ServerInfo serverInfo = GetServerInfo.decode(message);
            log.info("Received ServerInfo response from " + ctx.channel().remoteAddress() + ": " + serverInfo);
        }
        else if(message[1] == Protocol.S2S_ansAuthUser)
        {
            GetAuthUser.AuthUserResult result = GetAuthUser.decode(message);
            PendingAuthRequests.PendingAuthRequest pendingRequest = PendingAuthRequests.resolve(result.requestId());

            if(pendingRequest == null)
            {
                log.warn("Received S2S_ansAuthUser for an unknown or already-resolved request ID {}", result.requestId());
                return;
            }

            ClientSession clientSession = ClientSessionManager.getInstance().getSession(pendingRequest.ctx());
            if(clientSession != null)
            {
                clientSession.setAuthenticated(result.authenticated());

                if(result.authenticated())
                {
                    clientSession.setUsername(pendingRequest.username());
                }
            }

            pendingRequest.ctx().writeAndFlush(AnsAuthUser.createPacket(result.authenticated()));
        }
    }
}
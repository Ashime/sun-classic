package com.valiantgaming.webserver.server.handler;

import com.valiantgaming.webserver.database.entity.server.ServerInfo;
import com.valiantgaming.webserver.network.PendingCreateAccountRequests;
import com.valiantgaming.webserver.network.packet.server.AskServerInfo;
import com.valiantgaming.webserver.network.packet.server.handler.GetCreateAccount;
import com.valiantgaming.webserver.network.packet.server.handler.GetServerInfo;
import com.valiantgaming.webserver.network.session.server.ServerSession;
import com.valiantgaming.webserver.network.session.server.ServerSessionManager;
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
        // Wait for the mutual TLS handshake to finish before sending anything application-level - see
        // auth-server's ServerPacketHandler for why this replaced the old key-exchange packet flow.
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
        else if(message[1] == Protocol.S2S_ansCreateAccount)
        {
            GetCreateAccount.CreateAccountResult result = GetCreateAccount.decode(message);
            PendingCreateAccountRequests.resolve(result.requestId(), result.message());
        }
    }
}

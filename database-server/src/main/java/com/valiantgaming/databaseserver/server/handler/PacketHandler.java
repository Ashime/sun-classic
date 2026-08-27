package com.valiantgaming.databaseserver.server.handler;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.network.session.ServerSession;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.databaseserver.network.packet.server.AnsAuthUser;
import com.valiantgaming.databaseserver.network.packet.server.AnsCreateAccount;
import com.valiantgaming.databaseserver.network.packet.server.AnsServerInfo;
import com.valiantgaming.databaseserver.network.packet.server.handler.GetAuthUser;
import com.valiantgaming.databaseserver.network.packet.server.handler.GetCreateAccount;
import com.valiantgaming.databaseserver.network.session.ServerSessionManager;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class PacketHandler extends ChannelDuplexHandler
{
    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;
        log.info("Message: " + Utility.byteArrayToHexString(message));

        // The mutual TLS handshake (see NioServer) is what now authenticates and encrypts this connection; a
        // connection only reaches this handler at all once that succeeds, so the peer is already trusted here.
        switch(message[1])
        {
            case Protocol.S2S_askServerInfo ->
            {
                String serverName = null;

                if(message[2] == Category.AUTH)
                {
                    serverName = "GAME SERVER";

                } else if (message[2] == Category.GAME)
                {
                    serverName = "AUTH SERVER";

                } else if (message[2] == Category.WEBSITE)
                {
                    // WEB SERVER's own S2S traffic (S2S_askCreateAccount) goes straight to database-server -
                    // this is asking for the OTHER server it interacts with, same as the AUTH/GAME branches above.
                    serverName = "AUTH SERVER";
                }

                assert serverName != null;
                ctx.writeAndFlush(new AnsServerInfo().createPacket(serverName));
            }
            case Protocol.S2S_askAuthUser ->
            {
                GetAuthUser.AuthUserRequest request = GetAuthUser.decode(message);
                ctx.writeAndFlush(new AnsAuthUser().createPacket(request.requestId(), request.username(), request.password()));
            }
            case Protocol.S2S_askCreateAccount ->
            {
                GetCreateAccount.CreateAccountRequest request = GetCreateAccount.decode(message);
                ctx.writeAndFlush(new AnsCreateAccount().createPacket(request));
            }
            default ->
            {
                log.warn("Unknown packet! Packet: {}", Utility.byteArrayToHexString(message));
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        // Fires on ANY closure of this channel - whether the peer server closed it cleanly or the
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
}
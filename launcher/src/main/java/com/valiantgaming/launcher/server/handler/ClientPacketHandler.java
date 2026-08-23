package com.valiantgaming.launcher.server.handler;

import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.launcher.network.packet.server.AskUnknown1;
import com.valiantgaming.launcher.network.packet.server.handler.GetReady;
import com.valiantgaming.launcher.network.packet.server.handler.GetVerifyVersion;
import com.valiantgaming.launcher.network.session.ClientSession;
import com.valiantgaming.launcher.network.session.ClientSessionManager;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * Dispatches the launcher/AuthServer handshake: sends {@code L2A_askUnknown1} as soon as the
 * connection opens, then handles the two expected replies ({@code A2L_ansReady},
 * {@code A2L_ansVerifyVersion}). See {@code Protocol}'s block comment for the full flow this
 * is a shell of.
 */
@Log4j2
public class ClientPacketHandler extends ChannelDuplexHandler
{
    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        log.info("Connected to AuthServer at " + ctx.channel().remoteAddress());

        ClientSessionManager.getInstance().addSession(ctx);
        ctx.writeAndFlush(new AskUnknown1().createPacket());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;
        log.info("Message: " + Utility.byteArrayToHexString(message));

        if(message[1] == Protocol.A2L_ansReady)
        {
            byte[] teaKey = GetReady.decode(message);

            ClientSession session = ClientSessionManager.getInstance().getSession(ctx);
            session.setTeaKey(teaKey);
            session.setMessageCryptEnabled(true);

            log.info("Received ready response from AuthServer, TEA key length: " + teaKey.length);
        }
        else if(message[1] == Protocol.A2L_ansVerifyVersion)
        {
            GetVerifyVersion.VerifyVersion verifyVersion = GetVerifyVersion.decode(message);
            log.info("Received version verification from AuthServer: " + verifyVersion);

            // TODO: compare verifyVersion.launcherVersion()/clientVersion() against
            // LauncherConfig and surface a mismatch to the user once that UX exists.
        }
        else
        {
            log.warn("Unknown packet! Packet: {}", Utility.byteArrayToHexString(message));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        log.info("Connection to AuthServer at " + ctx.channel().remoteAddress() + " was closed.");
        ClientSessionManager.getInstance().removeSession(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        if(cause instanceof IOException)
        {
            // Expected when AuthServer isn't running yet - not an application bug.
            log.warn("Connection to AuthServer was reset: " + cause.getMessage());
        }
        else
        {
            log.error("Unexpected error on channel " + ctx.channel().remoteAddress(), cause);
        }

        ctx.close();
    }
}

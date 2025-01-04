package com.valiantgaming.databaseserver.server.handler;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.network.session.ServerSession;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.databaseserver.network.serverPacket.AnsAesFileKey;
import com.valiantgaming.databaseserver.network.serverPacket.AnsAesKey;
import com.valiantgaming.databaseserver.network.serverPacket.AnsRsaKey;
import com.valiantgaming.databaseserver.network.serverPacket.AnsServerInfo;
import com.valiantgaming.databaseserver.network.session.ServerSessionManager;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;

@Log4j2
public class PacketHandler extends ChannelDuplexHandler
{
    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        byte[] message = (byte[]) msg;
        log.info("Message: " + Utility.byteArrayToHexString(message));

        switch(message[1])
        {
            case Protocol.S2S_askAesFileKey ->
            {
                String loginChannelID = Utility.byteArrayToHexString(Utility.split(message, 2, message.length));

                log.info("Login Channel ID: " + loginChannelID);
                ctx.writeAndFlush(new AnsAesFileKey().createPacket(loginChannelID));
            }
            case Protocol.S2S_askRsaKey ->
            {
                ctx.writeAndFlush(new AnsRsaKey().createPacket((InetSocketAddress) ctx.channel().remoteAddress()));
            }
            case Protocol.S2S_askAesKey ->
            {
                if(ctx.writeAndFlush(new AnsAesKey().createPacket((InetSocketAddress) ctx.channel().remoteAddress())).isSuccess())
                {
                    ServerSession session = ServerSessionManager.getInstance().getSession((InetSocketAddress) ctx.channel().remoteAddress());
                    session.setMessageCryptEnabled(true);
                    ServerSessionManager.getInstance().updateSession(session);
                }
            }
            case Protocol.S2S_askServerInfo ->
            {
                String serverName = null;

                if(message[2] == Category.LOGIN)
                {
                    serverName = "LOGIN SERVER";

                } else if (message[2] == Category.GAME)
                {
                    serverName = "GAME SERVER";
                }

                assert serverName != null;
                ctx.writeAndFlush(new AnsServerInfo().createPacket(serverName));
            }
            default ->
            {
                log.warn("Unknown packet! Packet: " + Utility.byteArrayToHexString(message));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
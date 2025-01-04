package com.valiantgaming.authserver.server.handler;

import com.valiantgaming.authserver.network.packet.server.AskAesFileKey;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
public class ServerPacketHandler extends ChannelDuplexHandler
{
    private String dbChannelID;

//    @Override
//    public void channelRegistered(ChannelHandlerContext ctx) throws Exception
//    {
//        String[] address = ctx.channel().remoteAddress().toString().replace("/", "").split(":");
//        dbChannelID = ctx.channel().id().asLongText();
//
//        if(AuthServerConfig.getDbServerIp().equals(address[0]) && AuthServerConfig.getDbServerPort() == Integer.getInteger(address[1]))
//        {
//            log.info("Channel ID: " + dbChannelID);
//            ctx.writeAndFlush(new AskAesFileKey().createPacket(dbChannelID));
//        }
//    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        TimeUnit.SECONDS.sleep(5);

        dbChannelID = ctx.channel().id().asLongText();
        log.info("Channel ID: " + dbChannelID);
        ctx.writeAndFlush(new AskAesFileKey().createPacket(dbChannelID));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        byte[] message = (byte[]) msg;

        switch(message[1])
        {
            case Protocol.S2S_ansAesFileKey ->
            {
                log.info(Utility.byteArrayToHexString(message));
//                if(new GetAesFileKey())
//                {
//                    ;
//                }
            }
        }
    }
}
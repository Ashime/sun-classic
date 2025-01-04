package com.valiantgaming.databaseserver.server.coder;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.session.ServerSession;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.databaseserver.network.session.ServerSessionManager;
import com.valiantgaming.databaseserver.security.ServerPacketCrypt;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;

@Log4j2
public class PacketDecoder extends ChannelInboundHandlerAdapter implements Category
{
    private static final ServerPacketCrypt serverPacketCrypt = new ServerPacketCrypt();

    private static void decodePacket(ChannelHandlerContext ctx, byte[] packet)
    {
        ServerSession session = ServerSessionManager.getInstance().getSession((InetSocketAddress) ctx.channel().localAddress());

        int size;
        byte[] header;

        if(session.isPacketCryptEnabled())
        {
            packet = serverPacketCrypt.decryptPacket(session.getRsaPrivateKey().getEncoded(), packet);

            if(session.isMessageCryptEnabled())
            {
                packet = serverPacketCrypt.decryptMessage(session.getAesSecretKey().getEncoded(), session.getAesIv(), packet);
            }
        }

        // Flip the size bytes around.
        packet = Utility.flip(packet, 0, 1);
        // Split the size header off.
        header = Utility.split(packet, 0, 2);
        // Split message from size header
        packet = Utility.split(packet, 2, packet.length);

        // Convert size from byte array into int.
        size = Utility.byteArrayToInt(header);

        if(packet.length == size && packet[0] == Category.DATABASE)
        {
            log.info("Packet passed inbound check for size and category!");
            // Passes the message down the pipeline.
            ctx.fireChannelRead(packet);
        }
        else
        {
            log.warn("Packet dropped due to incorrect header! Category: " + packet[0] + "/tSize: " + size + "/" + packet.length + " (Calculated/Actual)");
            // Drops the packet.
            ctx.fireChannelReadComplete();
        }
    }

    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        decodePacket(ctx, (byte[]) msg);
    }
}
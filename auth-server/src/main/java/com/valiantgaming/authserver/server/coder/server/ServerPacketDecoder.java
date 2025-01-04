package com.valiantgaming.authserver.server.coder.server;

import com.valiantgaming.authserver.network.session.ServerSession;
import com.valiantgaming.authserver.network.session.ServerSessionManager;
import com.valiantgaming.authserver.security.PacketCrypt;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;

public class ServerPacketDecoder extends ChannelInboundHandlerAdapter
{
    private static void decodePacket(ChannelHandlerContext ctx, byte[] packet)
    {
        ServerSession session = ServerSessionManager.getInstance().getSession((InetSocketAddress) ctx.channel().localAddress());

        int size;
        byte[] header;

        if(session.isPacketCryptEnabled())
        {
            packet = PacketCrypt.decryptPacket(session.getRsaPublicKey().getEncoded(), packet);

            if(session.isMessageCryptEnabled())
            {
                packet = PacketCrypt.decryptMessage(session.getAesSecretKey().getEncoded(), session.getAesIv(), packet);
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

        // TODO: Update to include other server Category packets.
        if(packet.length == size && packet[0] == Category.DATABASE)
        {
            // Passes the message down the pipeline.
            ctx.fireChannelRead(packet);
        }
        else
        {
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
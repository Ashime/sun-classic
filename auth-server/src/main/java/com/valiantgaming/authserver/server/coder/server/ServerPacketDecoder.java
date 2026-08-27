package com.valiantgaming.authserver.server.coder.server;

import com.valiantgaming.authserver.network.session.server.ServerSession;
import com.valiantgaming.authserver.network.session.server.ServerSessionManager;
import com.valiantgaming.authserver.security.PacketCrypt;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerPacketDecoder extends ChannelInboundHandlerAdapter
{
    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        ServerSession session = ServerSessionManager.getInstance().getSession(ctx.channel().remoteAddress());

        int size;
        byte[] header;
        byte[] packet = (byte[]) msg;

        assert session != null;
        if(session.isPacketCryptEnabled())
        {
            packet = PacketCrypt.decryptPacket(session.getRsaPublicKey().getEncoded(), packet);

            if(session.isMessageCryptEnabled())
            {
                packet = PacketCrypt.decryptMessage(session.getAesSecretKey().getEncoded(), session.getAesIv(), packet);
            }
        }

        // Split the size header off.
        header = Utility.split(packet, 0, 2);
        // Split message from size header
        packet = Utility.split(packet, 2, packet.length);

        // Convert size from byte array into int.
        size = Utility.byteArrayToShort(header);

        // TODO: Update to include other server Category packets.
        if(packet.length == size && packet[0] == Category.DATABASE)
        {
            log.info("Packet passed inbound check for size and category!");
            // Passes the message down the pipeline.
            ctx.fireChannelRead(packet);
        }
        else
        {
            log.warn("Packet dropped due to incorrect header! Category: " + packet[0] + "\tSize: " + size + "/" + packet.length + " (Calculated/Actual)");
            // Drops the packet.
            ctx.fireChannelReadComplete();
        }
    }
}
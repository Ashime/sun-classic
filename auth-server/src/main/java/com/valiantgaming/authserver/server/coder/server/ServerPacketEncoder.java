package com.valiantgaming.authserver.server.coder.server;

import com.valiantgaming.authserver.network.session.server.ServerSession;
import com.valiantgaming.authserver.network.session.server.ServerSessionManager;
import com.valiantgaming.authserver.security.PacketCrypt;
import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.utility.Utility;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServerPacketEncoder extends ChannelOutboundHandlerAdapter implements Category
{
    private static void encodePacket(ChannelHandlerContext ctx, byte[] message)
    {
        ServerSession session = ServerSessionManager.getInstance().getSession(ctx.channel().remoteAddress());

        // TODO: Update to include other server Category packets.
        if(message[0] == Category.DATABASE)
        {
            // Little-endian header - see PacketFraming.
            byte[] header = Utility.intToByteArray((short) message.length);

            byte[] packet = new byte[header.length + message.length];
            System.arraycopy(header, 0, packet, 0, header.length);
            System.arraycopy(message, 0, packet, header.length, message.length);

            assert session != null;
            if(session.isMessageCryptEnabled())
            {
                packet = PacketCrypt.encryptMessage(session.getAesSecretKey().getEncoded(), session.getAesIv(), packet);
            }

            if(session.isPacketCryptEnabled())
            {
                packet = PacketCrypt.encryptPacket(session.getRsaPublicKey().getEncoded(), packet);
            }

            log.info(Utility.byteArrayToHexString(packet));
            ctx.writeAndFlush(packet);
        }
        else
        {
            log.error("Channel was flushed due to incorrect category! Server tried to send following packet: " + Utility.byteArrayToHexString(message));
            ctx.flush();
        }
    }

    @Override
    @SneakyThrows
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    {
        encodePacket(ctx, (byte[]) msg);
    }
}
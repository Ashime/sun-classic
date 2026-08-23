package com.valiantgaming.launcher.server.coder;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.launcher.network.session.ClientSession;
import com.valiantgaming.launcher.network.session.ClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.log4j.Log4j2;

/**
 * Prefixes an outbound {@code [category, protocol, ...data]} packet with its 2-byte
 * little-endian size header before it hits the wire.
 *
 * <p>Shell only: mirrors {@code auth-server}'s {@code ServerPacketEncoder} framing, but
 * doesn't encrypt anything yet - see {@link ClientPacketDecoder}'s class comment.
 */
@Log4j2
public class ClientPacketEncoder extends ChannelOutboundHandlerAdapter
{
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
    {
        byte[] message = (byte[]) msg;

        if(message[0] != Category.AUTH)
        {
            log.error("Channel was flushed due to incorrect category! Launcher tried to send following packet: " + Utility.byteArrayToHexString(message));
            ctx.flush();
            return;
        }

        byte[] header = Utility.intToByteArray((short) message.length);
        Utility.flip(header, 0, 1);

        byte[] packet = new byte[header.length + message.length];
        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(message, 0, packet, header.length, message.length);

        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);
        if(session != null && session.isMessageCryptEnabled())
        {
            // TODO: encrypt with TEA using session.getTeaKey() once TEA is implemented.
            log.warn("Sending packet while crypt is marked enabled, but TEA encryption isn't implemented yet.");
        }

        log.info(Utility.byteArrayToHexString(packet));
        ctx.writeAndFlush(packet, promise);
    }
}

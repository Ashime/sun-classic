package com.valiantgaming.launcher.server.coder;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.launcher.network.session.ClientSession;
import com.valiantgaming.launcher.network.session.ClientSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

/**
 * Strips the 2-byte little-endian size header off an inbound packet and validates it before
 * passing the remaining {@code [category, protocol, ...data]} bytes down to
 * {@link com.valiantgaming.launcher.server.handler.ClientPacketHandler}.
 *
 * <p>Shell only: mirrors {@code auth-server}'s {@code ServerPacketDecoder} framing.
 * {@link com.valiantgaming.commons.security.crypt.TEA} is implemented now, but the launcher
 * never sends a password, and the reference it was ported from only ever applies TEA to a
 * password field, not whole packets - so there's nothing for {@code messageCryptEnabled} to
 * actually gate on this connection.
 */
@Log4j2
public class ClientPacketDecoder extends ChannelInboundHandlerAdapter
{
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        ClientSession session = ClientSessionManager.getInstance().getSession(ctx);

        byte[] packet = (byte[]) msg;

        if(session != null && session.isMessageCryptEnabled())
        {
            // Nothing currently sets this true - see the class comment above.
            log.warn("Received packet while crypt is marked enabled, but nothing decrypts whole packets yet.");
        }

        // Split the size header off.
        byte[] header = Utility.split(packet, 0, 2);
        // Split message from size header.
        packet = Utility.split(packet, 2, packet.length);

        // Convert size from byte array into int.
        int size = Utility.byteArrayToShort(header);

        if(packet.length == size && packet[0] == Category.AUTH)
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

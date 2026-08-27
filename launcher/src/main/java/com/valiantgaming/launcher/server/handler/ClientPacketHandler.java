package com.valiantgaming.launcher.server.handler;

import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.launcher.network.PendingLoginRequest;
import com.valiantgaming.launcher.network.packet.server.AskUnknown1;
import com.valiantgaming.launcher.network.packet.server.handler.GetAuthReady;
import com.valiantgaming.launcher.network.packet.server.handler.GetAuthUser;
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
 * connection opens, then handles the expected replies ({@code A2L_ansReady},
 * {@code A2L_ansVerifyVersion}). See {@code Protocol}'s block comment for the full flow this
 * is a shell of.
 *
 * <p>Also handles two packets that, per auth-server's own {@code ClientPacketHandler} class
 * comment, it sends to <i>every</i> connection regardless of whether it's the launcher or a
 * real game client: {@code A2U_ansReady} (sent unconditionally on connect - this is what
 * actually carries the TEA key, unlike the empty {@code A2L_ansReady}) and
 * {@code A2U_ansAuthUser} (the reply to a login attempt this class sends via
 * {@code LoginController}).
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
            byte[] payload = GetReady.decode(message);

            // A2L_ansReady carries NO payload today (AnsLauncherReady sends category+opcode only),
            // and it arrives right after A2U_ansReady - which is the packet that actually delivers
            // the TEA key. Adopting this payload unconditionally therefore overwrote the real key
            // with an empty array, leaving login unable to encrypt a password. Only take it if a
            // capture ever shows this packet really does carry a key.
            if(payload.length > 0)
            {
                ClientSession session = ClientSessionManager.getInstance().getSession(ctx);
                session.setTeaKey(payload);

                log.info("Received ready response from AuthServer carrying a {}-byte key", payload.length);
            }
            else
            {
                log.info("Received ready response from AuthServer (no payload, as expected)");
            }
        }
        else if(message[1] == Protocol.A2L_ansVerifyVersion)
        {
            GetVerifyVersion.VerifyVersion verifyVersion = GetVerifyVersion.decode(message);
            log.info("Received version verification from AuthServer: " + verifyVersion);

            // TODO: compare verifyVersion.launcherVersion()/clientVersion() against
            // LauncherConfig and surface a mismatch to the user once that UX exists.
        }
        else if(message[1] == Protocol.A2U_ansReady)
        {
            byte[] teaKey = GetAuthReady.decode(message);

            ClientSession session = ClientSessionManager.getInstance().getSession(ctx);
            session.setTeaKey(teaKey);

            log.info("Received AuthServer's TEA key (A2U_ansReady), payload length: " + teaKey.length);
        }
        else if(message[1] == Protocol.A2U_ansAuthUser)
        {
            boolean authenticated = GetAuthUser.decode(message);
            log.info("Received login result from AuthServer: " + (authenticated ? "SUCCESS" : "FAILED"));

            PendingLoginRequest.resolve(authenticated);
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

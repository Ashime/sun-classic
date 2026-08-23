package com.valiantgaming.launcher.server;

import com.valiantgaming.launcher.config.LauncherConfig;
import com.valiantgaming.launcher.server.coder.ClientPacketDecoder;
import com.valiantgaming.launcher.server.coder.ClientPacketEncoder;
import com.valiantgaming.launcher.server.handler.ClientPacketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.log4j.Log4j2;

/**
 * Netty client for the launcher's handshake against AuthServer (see {@code Protocol}'s
 * {@code L2A_askUnknown1}/{@code A2L_ansReady}/{@code A2L_ansVerifyVersion} flow). Only ever
 * makes this one outbound connection - the launcher has no reason to accept inbound
 * connections, unlike the per-server {@code NioServer} classes elsewhere in this project that
 * also listen for game-client traffic.
 *
 * <p>Deliberately connects asynchronously and never throws/blocks on failure: unlike a
 * server's startup path, this runs from {@link com.valiantgaming.launcher.LauncherApplication
 * #init()} on the JavaFX launcher thread, and AuthServer isn't expected to be reachable in
 * most dev environments yet. A failed/refused connection is logged and otherwise ignored so
 * the launcher window still opens normally.
 */
@Log4j2
public class NioClient
{
    private static NioClient instance;
    private EventLoopGroup workerGroup;

    private NioClient()
    {
        connect();
    }

    private void connect()
    {
        String authServerIp = LauncherConfig.getAuthServerIp();
        int authServerPort = LauncherConfig.getAuthServerPort();
        int disconnect = LauncherConfig.getDisconnect();

        workerGroup = new NioEventLoopGroup(LauncherConfig.getAuthServerWorkerThreads());

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            protected void initChannel(SocketChannel ch)
            {
                // Inactivity Handler. Ping AuthServer every # seconds of inactivity and DC after # seconds.
                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, disconnect));

                // Encoder/Decoder for converting ByteBuf into byte[] and vice versa.
                ch.pipeline().addLast("byteDecoder", new ByteArrayDecoder());
                ch.pipeline().addLast("byteEncoder", new ByteArrayEncoder());

                // PacketDecoder checks, splits, and passes packets down.
                ch.pipeline().addLast("packetDecoder", new ClientPacketDecoder());
                // PacketEncoder checks, adds, and pushes packets up.
                ch.pipeline().addLast("packetEncoder", new ClientPacketEncoder());

                // PacketHandler dispatches packets to their corresponding classes.
                ch.pipeline().addLast("clientPacketHandler", new ClientPacketHandler());
            }
        }).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.connect(authServerIp, authServerPort).addListener(future ->
        {
            if(future.isSuccess())
            {
                log.info("Connecting to AuthServer at " + authServerIp + ":" + authServerPort);
            }
            else
            {
                log.warn("Unable to connect to AuthServer at " + authServerIp + ":" + authServerPort + " - " + future.cause().getMessage());
            }
        });
    }

    public void stop()
    {
        if(workerGroup != null)
            workerGroup.shutdownGracefully();

        log.info("NioClient has shut down.");
    }

    public static NioClient getInstance()
    {
        if(instance == null)
            synchronized (NioClient.class)
            {
                if(instance == null)
                    instance = new NioClient();
            }

        return instance;
    }
}

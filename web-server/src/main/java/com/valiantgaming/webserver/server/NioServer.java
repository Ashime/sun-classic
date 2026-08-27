package com.valiantgaming.webserver.server;

import com.valiantgaming.webserver.config.WebServerConfig;
import com.valiantgaming.webserver.network.firewall.server.IpRules;
import com.valiantgaming.webserver.network.session.server.ServerSessionManager;
import com.valiantgaming.webserver.server.coder.server.ServerPacketDecoder;
import com.valiantgaming.webserver.server.coder.server.ServerPacketEncoder;
import com.valiantgaming.webserver.server.handler.ServerPacketHandler;
import com.valiantgaming.commons.network.firewall.IpFilter;
import com.valiantgaming.commons.network.packet.PacketFraming;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * The S2S half only - unlike auth-server, web-server has no binary C2S listener of its own; game
 * clients never talk to it directly, and inbound traffic instead arrives as HTTP (see
 * {@code WebServerApplication}, which starts Spring's embedded servlet container separately).
 * Since Tomcat's own threads keep the JVM alive, {@link #initS2S()} runs on a background thread
 * rather than blocking the caller the way auth-server's does.
 */
@Log4j2
public class NioServer
{
    private static NioServer instance;
    private static final int disconnect = WebServerConfig.getDisconnect();

    private static EventLoopGroup serverWorker;

    public NioServer()
    {
        new Thread(this::initS2S, "s2s-client").start();
    }

    @SneakyThrows
    private void initS2S()
    {
        String dbServerIPAddress = WebServerConfig.getDbServerIp();
        int dbServerPort = WebServerConfig.getDbServerPort();

        String serverIpAddress = WebServerConfig.getServerIp();
        int serverPort = WebServerConfig.getServerPort();
        int serverWorkingThreads = WebServerConfig.getServerWorkingThreads();

        serverWorker = new NioEventLoopGroup(serverWorkingThreads);

        // Mutual TLS: this connection only completes if the Database Server presents a certificate signed by our
        // trusted CA, and we present one it trusts in return.
        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(new File(WebServerConfig.getTlsCertPath()), new File(WebServerConfig.getTlsKeyPath()))
                .trustManager(new File(WebServerConfig.getTlsCaPath()))
                .build();

        Bootstrap serverBootstrap = new Bootstrap();
        serverBootstrap.group(serverWorker).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch)
            {
                ch.pipeline().addLast("ssl", sslContext.newHandler(ch.alloc(), dbServerIPAddress, dbServerPort));

                ch.pipeline().addLast("ipFilter", new IpFilter(false, IpRules.getInstance()));

                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, disconnect));

                // Reassembles the 2-byte-length-prefixed frame each PacketEncoder writes, so a
                // frame split across TCP reads (or several coalesced into one) always reaches
                // byteDecoder as exactly one frame - see PacketFraming's class comment.
                ch.pipeline().addLast("frameDecoder", PacketFraming.newFrameDecoder());

                ch.pipeline().addLast("byteDecoder", new ByteArrayDecoder());
                ch.pipeline().addLast("byteEncoder", new ByteArrayEncoder());

                ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                ch.pipeline().addLast("packetDecoder", new ServerPacketDecoder());
                ch.pipeline().addLast("packetEncoder", new ServerPacketEncoder());

                ch.pipeline().addLast("serverPacketHandler", new ServerPacketHandler());
            }

        }).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.AUTO_READ, true).option(ChannelOption.SO_KEEPALIVE, true);

        serverBootstrap.localAddress(serverIpAddress, serverPort);

        ChannelFuture cf = serverBootstrap.connect(dbServerIPAddress, dbServerPort).sync();

        if(cf.isSuccess())
        {
            log.info("Web Server successfully connected to " + dbServerIPAddress + ":" + dbServerPort);
            ServerSessionManager.setChannel(cf.channel());

            while(cf.channel().isRegistered())
            {
                if(!cf.channel().isOpen())
                {
                    log.error("Connection to " + dbServerIPAddress + ":" + dbServerPort + " was closed! Compare ServerInfo data to server ini files!");
                    TimeUnit.SECONDS.sleep(1);
                }
            }
        }
        else
        {
            log.error("Web Server failed to connect to " + dbServerIPAddress + ":" + dbServerPort);
        }
    }

    public void stop()
    {
        if(serverWorker != null)
            serverWorker.shutdownGracefully();

        ServerSessionManager.getInstance().clearSessions();
        ServerSessionManager.setChannel(null);

        log.info("Web Server S2S client has successfully shutdown!");
    }

    public static NioServer getInstance()
    {
        if(instance == null)
            synchronized (NioServer.class)
            {
                if(instance == null)
                    instance = new NioServer();
            }

        return instance;
    }
}

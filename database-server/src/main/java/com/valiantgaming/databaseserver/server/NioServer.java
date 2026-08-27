package com.valiantgaming.databaseserver.server;

import com.valiantgaming.commons.network.firewall.IpFilter;
import com.valiantgaming.commons.network.packet.PacketFraming;
import com.valiantgaming.databaseserver.config.DatabaseServerConfig;
import com.valiantgaming.databaseserver.network.firewall.IpRules;
import com.valiantgaming.databaseserver.server.coder.PacketDecoder;
import com.valiantgaming.databaseserver.server.coder.PacketEncoder;
import com.valiantgaming.databaseserver.server.handler.PacketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ipfilter.UniqueIpFilter;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.File;

@Log4j2
public class NioServer
{
    private final EventLoopGroup acceptor;
    private final EventLoopGroup worker;

    // NOTE: https://netty.io/wiki/new-and-noteworthy-in-5.0.html

    @SneakyThrows
    public NioServer()
    {
        int disconnect = DatabaseServerConfig.getDisconnect();
        String ipAddress = DatabaseServerConfig.getIpAddress();
        int port = DatabaseServerConfig.getPort();
        int acceptThreads = DatabaseServerConfig.getAcceptThreads();
        int workingThreads = DatabaseServerConfig.getWorkingThreads();

        acceptor = new NioEventLoopGroup(acceptThreads);
        worker = new NioEventLoopGroup(workingThreads);

        // Mutual TLS: clientAuth(REQUIRE) means the handshake fails closed unless the connecting server presents a
        // certificate signed by our trusted CA. This is the peer authentication IpRules could never really provide.
        SslContext sslContext = SslContextBuilder
                .forServer(new File(DatabaseServerConfig.getTlsCertPath()), new File(DatabaseServerConfig.getTlsKeyPath()))
                .trustManager(new File(DatabaseServerConfig.getTlsCaPath()))
                .clientAuth(ClientAuth.REQUIRE)
                .build();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(acceptor, worker).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            protected void initChannel(SocketChannel ch)
            {
                // Added first so every byte crossing the wire is encrypted before any other handler sees it.
                ch.pipeline().addLast("ssl", sslContext.newHandler(ch.alloc()));

                ch.pipeline().addLast("ipFilter", new IpFilter(false, IpRules.getInstance()));
                // UniqueIpFilter only allows one IP per channel, so a client cannot connect more than once.
                ch.pipeline().addLast("uniqueIpFilter", new UniqueIpFilter());

                // Inactivity Handler. Ping the client every # seconds of inactivity and DC client after # seconds.
                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, disconnect));

                // Reassembles the 2-byte-length-prefixed frame each PacketEncoder writes, so a
                // frame split across TCP reads (or several coalesced into one) always reaches
                // byteDecoder as exactly one frame - see PacketFraming's class comment.
                ch.pipeline().addLast("frameDecoder", PacketFraming.newFrameDecoder());

                // Encoder/Decoder for converting ByteBuf into byte[] and vise versa.
                ch.pipeline().addLast("byteDecoder", new ByteArrayDecoder());
                ch.pipeline().addLast("byteEncoder", new ByteArrayEncoder());

                ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                // PacketDecoder checks, split, and passes packets down.
                ch.pipeline().addLast("packetDecoder", new PacketDecoder());
                // PacketEncoder checks, adds, and pushes packets up.
                ch.pipeline().addLast("packetEncoder", new PacketEncoder());

                // Packet Handler will check and pass the packets their corresponding classes.
                ch.pipeline().addLast("packetHandler", new PacketHandler());

            }
        }).childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.AUTO_READ, true);

        /*ChannelFuture serverChannel =*/ bootstrap.bind(ipAddress, port).sync();
        log.info("Server connection address - {}:{}", ipAddress, port);
    }

//    public void stop()
//    {
//        if (acceptor != null)
//            acceptor.shutdownGracefully();
//
//        if (worker != null)
//            worker.shutdownGracefully();
//
//        log.info("Server has successfully shutdown!");
//    }

    private static final class InstanceHolder {
        private static final NioServer instance = new NioServer();
    }

    public static NioServer getInstance()
    {
        return InstanceHolder.instance;
    }
}
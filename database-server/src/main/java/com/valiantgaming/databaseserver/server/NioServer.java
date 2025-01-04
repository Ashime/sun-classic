package com.valiantgaming.databaseserver.server;

import com.valiantgaming.commons.network.firewall.IpFilter;
import com.valiantgaming.databaseserver.config.DatabaseServerConfig;
import com.valiantgaming.databaseserver.network.firewall.IpRules;
import com.valiantgaming.databaseserver.server.coder.PacketDecoder;
import com.valiantgaming.databaseserver.server.coder.PacketEncoder;
import com.valiantgaming.databaseserver.server.handler.PacketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ipfilter.UniqueIpFilter;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NioServer
{
    private static NioServer instance;
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

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(acceptor, worker).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer()
        {
            @Override
            protected void initChannel(Channel ch)
            {

                ch.pipeline().addLast("ipFilter", new IpFilter(false, IpRules.getInstance()));
                // UniqueIpFilter only allows one IP per channel, so a client cannot connect more than once.
                ch.pipeline().addLast("uniqueIpFilter", new UniqueIpFilter());

                // Inactivity Handler. Ping the client every # seconds of inactivity and DC client after # seconds.
                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, disconnect));

                // Encoder/Decoder for converting ByteBuf into byte[] and vise versa.
                ch.pipeline().addLast("byteDecoder", new ByteArrayDecoder());
                ch.pipeline().addLast("byteEncoder", new ByteArrayEncoder());

                // PacketDecoder checks, split, and passes packets down.
                ch.pipeline().addLast("packetDecoder", new PacketDecoder());
                // PacketEncoder checks, adds, and pushes packets up.
                ch.pipeline().addLast("packetEncoder", new PacketEncoder());

                // Packet Handler will check and pass the packets their corresponding classes.
                ch.pipeline().addLast("packetHandler", new PacketHandler());

            }
        }).childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.AUTO_READ, true);

        ChannelFuture serverChannel = bootstrap.bind(ipAddress, port).sync();
        log.info("Server connection address - " + ipAddress + ":" + port);
    }

    public void stop()
    {
        if (acceptor != null)
            acceptor.shutdownGracefully();

        if (worker != null)
            worker.shutdownGracefully();

        log.info("Server has successfully shutdown!");
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
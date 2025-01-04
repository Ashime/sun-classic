package com.valiantgaming.authserver.server;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.authserver.network.firewall.server.IpRules;
import com.valiantgaming.authserver.server.coder.server.ServerPacketDecoder;
import com.valiantgaming.authserver.server.coder.server.ServerPacketEncoder;
import com.valiantgaming.authserver.server.handler.ServerPacketHandler;
import com.valiantgaming.commons.network.firewall.IpFilter;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ipfilter.UniqueIpFilter;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
public class NioServer
{
    private static NioServer instance;
    private static final int disconnect = AuthServerConfig.getDisconnect();

    private static EventLoopGroup serverWorker;

    private static EventLoopGroup clientAcceptor;
    private static EventLoopGroup clientWorker;

    // NOTE: https://netty.io/wiki/new-and-noteworthy-in-5.0.html

    @SneakyThrows
    public NioServer()
    {
        initS2S();
//        Thread.sleep(120000);
//        initC2S();
    }

    @SneakyThrows
    private void initS2S()
    {
        // DATABASE SERVER
        String dbServerIPAddress = AuthServerConfig.getDbServerIp();
        int dbServerPort = AuthServerConfig.getDbServerPort();

        // SERVER TO SERVER
        String serverIpAddress = AuthServerConfig.getServerIp();
        int serverPort = AuthServerConfig.getServerPort();
        int serverAcceptThreads = AuthServerConfig.getServerAcceptThreads();
        int serverWorkingThreads = AuthServerConfig.getServerWorkingThreads();
        serverWorker = new NioEventLoopGroup(serverWorkingThreads);

        Bootstrap serverBootstrap = new Bootstrap();
        serverBootstrap.group(serverWorker).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception
            {
                ChannelPipeline pipeline = ch.pipeline();

                ch.pipeline().addLast("ipFilter", new IpFilter(false, IpRules.getInstance()));

                // Inactivity Handler. Ping the client every # seconds of inactivity and DC client after # seconds.
                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, disconnect));

                // Encoder/Decoder for converting ByteBuf into byte[] and vise versa.
                ch.pipeline().addLast("byteDecoder", new ByteArrayDecoder());
                ch.pipeline().addLast("byteEncoder", new ByteArrayEncoder());

                // PacketDecoder checks, split, and passes packets down.
                ch.pipeline().addLast("packetDecoder", new ServerPacketDecoder());
                // PacketEncoder checks, adds, and pushes packets up.
                ch.pipeline().addLast("packetEncoder", new ServerPacketEncoder());

                // Packet Handler will check and pass the packets their corresponding classes.
                ch.pipeline().addLast("serverPacketHandler", new ServerPacketHandler());
            }

        }).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.AUTO_READ, true);

        serverBootstrap.localAddress(serverIpAddress, serverPort);
        log.info("Server bind to " + serverIpAddress + ":" + serverPort);

        ChannelFuture cf = serverBootstrap.connect(dbServerIPAddress, dbServerPort).sync();

        if(cf.isSuccess())
        {
            log.info("Server successfully connected to " + dbServerIPAddress + ":" + dbServerPort);

            while(cf.channel().isRegistered())
            {
                if(!cf.channel().isOpen())
                {
                    log.error("Connection to " + dbServerIPAddress + ":" + dbServerPort+ " was closed! Compare ServerInfo data to server ini files!");
                    TimeUnit.SECONDS.sleep(1);
                }
            }
        }
        else
            log.error("Server failed to connect to " + dbServerIPAddress + ":" + dbServerPort);
    }

    @SneakyThrows
    private void initC2S()
    {
        // CLIENT TO SERVER
        String clientIpAddress = AuthServerConfig.getClientIp();
        int clientPort = AuthServerConfig.getClientPort();
        int clientAcceptThreads = AuthServerConfig.getClientAcceptThreads();
        int clientWorkingThreads = AuthServerConfig.getClientWorkingThreads();
        clientAcceptor = new NioEventLoopGroup(clientAcceptThreads);
        clientWorker = new NioEventLoopGroup(clientWorkingThreads);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(clientAcceptor, clientWorker).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer()
        {
            @Override
            protected void initChannel(Channel ch)
            {

//                ch.pipeline().addLast("ipFilter", new IpFilter());
                // UniqueIpFilter only allows one IP per channel, so a client cannot connect more than once.
                ch.pipeline().addLast("uniqueIpFilter", new UniqueIpFilter());

                // Inactivity Handler. Ping the client every # seconds of inactivity and DC client after # seconds.
                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, disconnect));

                // Encoder/Decoder for converting ByteBuf into byte[] and vise versa.
                ch.pipeline().addLast("byteDecoder", new ByteArrayDecoder());
                ch.pipeline().addLast("byteEncoder", new ByteArrayEncoder());

//                // PacketDecoder checks, split, and passes packets down.
//                ch.pipeline().addLast("packetDecoder", new PacketDecoder());
//                // PacketEncoder checks, adds, and pushes packets up.
//                ch.pipeline().addLast("packetEncoder", new PacketEncoder());
//
//                // Packet Handler will check and pass the packets their corresponding classes.
//                ch.pipeline().addLast("clientPacketHandler", new ClientPacketHandler());

            }
        }).childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.AUTO_READ, true);

        ChannelFuture serverChannel = serverBootstrap.bind(clientIpAddress, clientPort).sync();
        log.info("Server connection address - " + clientIpAddress + ":" + clientPort);
    }

    public void stop()
    {
        if (clientAcceptor != null)
            clientAcceptor.shutdownGracefully();

        if (clientWorker != null)
            clientWorker.shutdownGracefully();

        if(serverWorker != null)
            serverWorker.shutdownGracefully();

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
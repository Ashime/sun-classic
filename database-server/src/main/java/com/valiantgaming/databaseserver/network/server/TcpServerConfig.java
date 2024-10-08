package com.valiantgaming.databaseserver.network.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.messaging.MessageChannel;

// https://docs.spring.io/spring-integration/reference/html/ip.html#testing-connections

@EnableIntegration
@Configuration
public class TcpServerConfig
{
    @Value("${server.ipAddress}")
    private String ip;

    @Value("${server.port}")
    private int port;

    @Value("${server.s2s.bufferSize}")
    private int bufferSize;

    @Value("${server.s2s.connectionTimeOut}")
    private int timeOut;

    @Bean
    public TcpNioServerConnectionFactory serverConnectionFactory()
    {
        TcpNioServerConnectionFactory serverConnectionFactory = new TcpNioServerConnectionFactory(port);
        serverConnectionFactory.setLocalAddress(ip);

        serverConnectionFactory.setUsingDirectBuffers(true);
        serverConnectionFactory.setSoReceiveBufferSize(bufferSize);

        serverConnectionFactory.setMultiAccept(true);
        serverConnectionFactory.setSoTcpNoDelay(true);
        serverConnectionFactory.setSoTimeout(timeOut);

        return serverConnectionFactory;
    }

    @Bean
    public MessageChannel inboundChannel()
    {
        return new DirectChannel();
    }

    @Bean
    public TcpInboundGateway inboundGateway(AbstractServerConnectionFactory serverConnectionFactory, MessageChannel inboundChannel)
    {
        TcpInboundGateway tcpInboundGateway = new TcpInboundGateway();
        tcpInboundGateway.setConnectionFactory(serverConnectionFactory);
        tcpInboundGateway.setRequestChannel(inboundChannel);

        return tcpInboundGateway;
    }
}
package com.valiantgaming.databaseserver.network.server;

import com.valiantgaming.databaseserver.network.server.config.TcpServerConfig;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.serializer.TcpCodecs;

@NoArgsConstructor
public class NioServer
{
    @Setter
    private static String ipAddress;
    @Setter
    private static int port;
    @Setter
    private static int acceptThreads;
    @Setter
    private static int workingThreads;
    @Setter
    private static int bufferSize;
    @Setter
    private static int connectionTimeout;

    private TcpServerConfig tcpServerConfig;
    private TcpReceivingChannelAdapter receivingChannelAdapter;
    private TcpInboundGateway inboundGateway;

    public void init()
    {
        tcpServerConfig = new TcpServerConfig();

        receivingChannelAdapter = new TcpReceivingChannelAdapter();
        receivingChannelAdapter.setConnectionFactory(tcpServerConfig.serverConnectionFactory());
        receivingChannelAdapter.setOutputChannel(tcpServerConfig.inboundChannel());


//        inboundGateway = tcpServerConfig.inboundGateway(tcpServerConfig.serverConnectionFactory(),
//                tcpServerConfig.inboundChannel());
//
//        inboundGateway.start();
    }

    @Bean
    public IntegrationFlow server() {
        return IntegrationFlow.from(Tcp.inboundAdapter(Tcp.netServer(1234)
                                .deserializer(TcpCodecs.lengthHeader1())
                                .backlog(30))
                        .errorChannel("tcpIn.errorChannel")
                        .id("tcpIn"))
                .transform(Transformers.objectToString())
                .channel("tcpInbound")
                .get();
    }
}
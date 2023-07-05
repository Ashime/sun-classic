package com.valiantgaming.databaseserver.network.server;

import com.valiantgaming.databaseserver.network.server.service.MessageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

// https://github.com/zhwxp/spring-boot-tcp-server-sample/blob/master/src/main/java/com/zhwxp/sample/spring/boot/tcp/server/endpoint/TcpServerEndpoint.java

@Log4j2
@MessageEndpoint
public class TcpServerEndpoint
{
    private final MessageService messageService;

    @Autowired
    public TcpServerEndpoint(MessageService messageService)
    {
        this.messageService = messageService;
    }

    @ServiceActivator(inputChannel = "inboundChannel")
    public byte[] process(byte[] message)
    {
        return messageService.processMessage(message);
    }
}
package com.valiantgaming.databaseserver.network.server.service.impl;

import com.valiantgaming.databaseserver.network.server.service.MessageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Log4j2
@Service
public class MessageServiceImpl implements MessageService
{
    @Override
    public byte[] processMessage(byte[] message)
    {
        String messageContent = new String(message);
        String responseContent = String.format("Inbound Message: " + messageContent);

        return responseContent.getBytes(StandardCharsets.UTF_8);
    }
}
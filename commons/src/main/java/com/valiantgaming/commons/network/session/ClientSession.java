package com.valiantgaming.commons.network.session;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClientSession
{
    private int accountID;
    private int accountStorageID;
    private int characterSlotID;

    // Client packet encryption key.
    private byte[] teaKey;

    // TODO: Any additional encryption keys.

    // Inbound and outbound
    private ChannelHandlerContext duplexCtx;
}
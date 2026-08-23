package com.valiantgaming.databaseserver.security;

import com.valiantgaming.commons.security.PacketCrypt;
import jakarta.validation.constraints.NotNull;

public class ServerPacketCrypt extends PacketCrypt
{
    @Override
    public byte[] decryptPacket(@NotNull byte[] key, @NotNull byte[] message)
    {
        return new byte[]{0x00};
    }

    @Override
    public byte[] encryptPacket(@NotNull byte[] key, @NotNull byte[] message)
    {
        return new byte[]{0x00};
    }
}
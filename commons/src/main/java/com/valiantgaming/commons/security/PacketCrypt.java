package com.valiantgaming.commons.security;

import jakarta.validation.constraints.NotNull;

public abstract class PacketCrypt
{
    public abstract byte[] decryptPacket(@NotNull byte[] key, @NotNull byte[] message);

    public abstract byte[] encryptPacket(@NotNull byte[] key, @NotNull byte[] message);
}
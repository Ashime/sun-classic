package com.valiantgaming.commons.network.session;

import lombok.Getter;
import lombok.Setter;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

@Getter @Setter
public class ServerSession
{
    public Object serverInfo;
    private PrivateKey rsaPrivateKey;
    private PublicKey rsaPublicKey;
    private SecretKey aesSecretKey;
    private byte[] aesIv;
    private boolean packetCryptEnabled = false;
    private boolean messageCryptEnabled = false;
}
package com.valiantgaming.authserver.network.session;

import com.valiantgaming.authserver.database.entity.ServerInfo;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public class ServerSession
{
    @Getter @Setter
    private ServerInfo serverInfo;
    @Getter @Setter
    private PublicKey rsaPublicKey;

    @Getter @Setter
    private SecretKey aesSecretKey;

    @Getter @Setter
    private byte[] aesIv;

    @Getter @Setter
    private boolean packetCryptEnabled = false;

    @Getter @Setter
    private boolean messageCryptEnabled = false;
}
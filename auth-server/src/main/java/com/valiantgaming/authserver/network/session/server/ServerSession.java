package com.valiantgaming.authserver.network.session.server;

import com.valiantgaming.authserver.database.entity.server.ServerInfo;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Arrays;

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

    @Override
    public String toString() {
        return "ServerSession{" +
                "serverInfo=" + serverInfo +
                ", rsaPublicKey=" + rsaPublicKey +
                ", aesSecretKey=" + aesSecretKey +
                ", aesIv=" + Arrays.toString(aesIv) +
                ", packetCryptEnabled=" + packetCryptEnabled +
                ", messageCryptEnabled=" + messageCryptEnabled +
                '}';
    }
}
package com.valiantgaming.databaseserver.network.serverPacket;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.network.session.ServerSession;
import com.valiantgaming.commons.security.hash.SHA;
import com.valiantgaming.commons.utility.Utility;
import com.valiantgaming.databaseserver.network.session.ServerSessionManager;

import java.net.InetSocketAddress;

public class AnsRsaKey
{
    public byte[] createPacket(InetSocketAddress remoteAddress)
    {
        ServerSession session = ServerSessionManager.getInstance().getSession(remoteAddress);
        assert session != null;

        byte[] publicKey = session.getRsaPublicKey().getEncoded();
        byte[] mac = SHA.getMac(Utility.byteArrayToHexString(publicKey));

        byte[] packet = new byte[publicKey.length + mac.length + 2];
        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansRsaKey;

        System.arraycopy(publicKey, 0, packet, 2, publicKey.length);
        System.arraycopy(mac, 0, packet, publicKey.length, mac.length);

        return packet;
    }
}
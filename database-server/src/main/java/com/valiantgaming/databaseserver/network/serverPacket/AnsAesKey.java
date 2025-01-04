package com.valiantgaming.databaseserver.network.serverPacket;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.network.session.ServerSession;
import com.valiantgaming.databaseserver.network.session.ServerSessionManager;

import java.net.InetSocketAddress;

public class AnsAesKey
{
    public byte[] createPacket(InetSocketAddress remoteAddress)
    {
        ServerSession session = ServerSessionManager.getInstance().getSession(remoteAddress);
        assert session != null;

        byte[] aesKey = session.getAesSecretKey().getEncoded();

        byte[] packet = new byte[aesKey.length + session.getAesIv().length + 2];
        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansAesKey;

        System.arraycopy(aesKey, 0, packet, 2, aesKey.length);
        System.arraycopy(session.getAesIv(), 0, packet, (aesKey.length + 2), session.getAesIv().length);

        session.setPacketCryptEnabled(true);
        ServerSessionManager.getInstance().updateSession(session);

        return packet;
    }
}
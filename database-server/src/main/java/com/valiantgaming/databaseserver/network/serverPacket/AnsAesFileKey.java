package com.valiantgaming.databaseserver.network.serverPacket;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.security.hash.SHA;
import com.valiantgaming.databaseserver.database.HibernateSession;
import com.valiantgaming.databaseserver.database.dao.EncryptionKeyDAO;

import java.nio.charset.StandardCharsets;

public class AnsAesFileKey
{
    public byte[] createPacket(String channelID)
    {
        EncryptionKeyDAO encKeyDAO = new EncryptionKeyDAO(HibernateSession.createSession());

        byte[] aesKey = encKeyDAO.getKey("AES-256 KEY").getKeyValue().getBytes(StandardCharsets.UTF_8);
        byte[] aesIvKey = encKeyDAO.getKey("AES-256 IV").getKeyValue().getBytes(StandardCharsets.UTF_8);
        byte[] macKey = SHA.getMac(channelID);

        byte[] packet = new byte[aesKey.length + aesIvKey.length + macKey.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansAesFileKey;

        System.arraycopy(aesKey, 0, packet, 2, aesKey.length);
        System.arraycopy(aesIvKey, 0, packet, aesKey.length, aesIvKey.length);
        System.arraycopy(macKey, 0, packet, (packet.length - macKey.length), macKey.length);

        return packet;
    }
}
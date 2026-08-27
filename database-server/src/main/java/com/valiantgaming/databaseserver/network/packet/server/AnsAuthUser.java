package com.valiantgaming.databaseserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.databaseserver.database.DatabaseManager;
import com.valiantgaming.databaseserver.database.HibernateSession;
import com.valiantgaming.databaseserver.database.dao.account.AccountDAO;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.Future;

@Log4j2
public class AnsAuthUser
{
    @SneakyThrows
    public byte[] createPacket(int requestId, String username, String password)
    {
        boolean authenticated = false;

        // A database failure here (a missing GRANT on AuthenticateAccount/GetAccountCredentials, a
        // dropped pool connection, a stored procedure error) must NOT propagate: this runs on the
        // Netty event-loop thread, so an escaping exception reaches PacketHandler.exceptionCaught,
        // which closes the channel - taking down the single shared S2S connection that every other
        // server depends on, and leaving the caller waiting for a reply that can never arrive.
        // Treat it as "not authenticated" instead and keep the connection alive.
        try(AccountDAO accountDAO = new AccountDAO(HibernateSession.createSession()))
        {
            Future<Object> futureTask = DatabaseManager.getInstance()
                    .submitTask(() -> accountDAO.authenticateAccount(username, password));

            authenticated = (boolean) futureTask.get();
        }
        catch(Exception e)
        {
            log.error("Failed to authenticate '{}' - rejecting the login attempt", username, e);
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try(DataOutputStream data = new DataOutputStream(byteStream))
        {
            data.writeInt(requestId);
            data.writeBoolean(authenticated);
        }

        byte[] data = byteStream.toByteArray();
        byte[] packet = new byte[data.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansAuthUser;
        System.arraycopy(data, 0, packet, 2, data.length);

        return packet;
    }
}

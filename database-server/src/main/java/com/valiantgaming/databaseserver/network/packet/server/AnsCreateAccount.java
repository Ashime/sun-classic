package com.valiantgaming.databaseserver.network.packet.server;

import com.valiantgaming.commons.network.packet.Category;
import com.valiantgaming.commons.network.packet.Protocol;
import com.valiantgaming.commons.security.crypt.ARGON2;
import com.valiantgaming.databaseserver.database.DatabaseManager;
import com.valiantgaming.databaseserver.database.HibernateSession;
import com.valiantgaming.databaseserver.database.dao.account.AccountDAO;
import com.valiantgaming.databaseserver.database.entity.Profile;
import com.valiantgaming.databaseserver.database.entity.account.Account;
import com.valiantgaming.databaseserver.network.packet.server.handler.GetCreateAccount.CreateAccountRequest;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.Future;

@Log4j2
public class AnsCreateAccount
{
    @SneakyThrows
    public byte[] createPacket(CreateAccountRequest request)
    {
        Account account = new Account();
        account.setUsername(request.username());
        // The password arrives over S2S as plaintext (same as AuthUser's flow) and is only ever
        // hashed here, on database-server - no other server should ever hold or store a raw hash.
        account.setPassword(new ARGON2().hashPassword(request.password()));
        account.setEmail(request.email());

        Profile profile = new Profile();
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setBirthMonth((short) request.birthMonth());
        profile.setBirthDay((short) request.birthDay());
        profile.setSecurityQuestion1(request.securityQ1());
        profile.setAnswer1(request.answer1());
        profile.setSecurityQuestion2(request.securityQ2());
        profile.setAnswer2(request.answer2());
        profile.setSecurityQuestion3(request.securityQ3());
        profile.setAnswer3(request.answer3());

        String message;

        // Same reasoning as AnsAuthUser: an escaping database exception would reach
        // PacketHandler.exceptionCaught and close the shared S2S connection, so a failed
        // insert is reported back as a normal failure message instead of being thrown.
        try(AccountDAO accountDAO = new AccountDAO(HibernateSession.createSession()))
        {
            Future<Object> futureTask = DatabaseManager.getInstance()
                    .submitTask(() -> accountDAO.createAccount(account, profile));

            boolean created = (boolean) futureTask.get();
            message = created ? "SUCCESS!"
                    : accountDAO.getResultMessage() != null ? accountDAO.getResultMessage() : "FAILED: ACCOUNT CREATION FAILED!";
        }
        catch(Exception e)
        {
            log.error("Failed to create account '{}'", request.username(), e);
            message = "FAILED: ACCOUNT CREATION FAILED!";
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try(DataOutputStream data = new DataOutputStream(byteStream))
        {
            data.writeInt(request.requestId());
            data.writeUTF(message);
        }

        byte[] data = byteStream.toByteArray();
        byte[] packet = new byte[data.length + 2];

        packet[0] = Category.DATABASE;
        packet[1] = Protocol.S2S_ansCreateAccount;
        System.arraycopy(data, 0, packet, 2, data.length);

        return packet;
    }
}

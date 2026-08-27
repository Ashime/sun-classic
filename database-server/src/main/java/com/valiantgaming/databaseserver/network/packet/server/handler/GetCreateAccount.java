package com.valiantgaming.databaseserver.network.packet.server.handler;

import com.valiantgaming.commons.utility.Utility;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class GetCreateAccount
{
    public record CreateAccountRequest(int requestId, String username, String password, String email,
                                        String firstName, String lastName, int birthMonth, int birthDay,
                                        String securityQ1, String answer1, String securityQ2, String answer2,
                                        String securityQ3, String answer3) {}

    @SneakyThrows
    public static CreateAccountRequest decode(byte[] message)
    {
        byte[] payload = Utility.split(message, 2, message.length);

        try(DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload)))
        {
            int requestId = data.readInt();
            String username = data.readUTF();
            String password = data.readUTF();
            String email = data.readUTF();
            String firstName = data.readUTF();
            String lastName = data.readUTF();
            int birthMonth = data.readInt();
            int birthDay = data.readInt();
            String securityQ1 = data.readUTF();
            String answer1 = data.readUTF();
            String securityQ2 = data.readUTF();
            String answer2 = data.readUTF();
            String securityQ3 = data.readUTF();
            String answer3 = data.readUTF();

            return new CreateAccountRequest(requestId, username, password, email, firstName, lastName,
                    birthMonth, birthDay, securityQ1, answer1, securityQ2, answer2, securityQ3, answer3);
        }
    }
}

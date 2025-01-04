package com.valiantgaming.authserver.network.packet.client.handler;

import java.util.Arrays;

public class AuthUser
{
    public static boolean authUserAndPassword(byte[] input, byte[] key, String ipAddress)
    {
        byte[] username = Arrays.copyOfRange(input, 6, 56);
//        byte[] shortenUsername = Arrays.copyOfRange(username, 0, Utility.indexOf(username, "0"));
        byte[] password = Arrays.copyOfRange(input, 57, 80); //56-79
//        byte[] dPassword = Tea.passwordDecode(password, key);
//
//        if (IniFile.isTrustedDevices())
//        {
//            query.getStoredTrustedDevices(Convert.byteArrayToUTF8String(shortenUsername));
//
//            for (String temp : Query.getTrustedDevices())
//            {
//                if (temp.matches(ipAddress))
//                    return query.authUser(Convert.byteArrayToUTF8String(shortenUsername), Convert.byteArrayToUTF8String(dPassword));
//            }
//
//            return false;
//        }
//        else
//            return query.authUser(Convert.byteArrayToUTF8String(shortenUsername), Convert.byteArrayToUTF8String(dPassword));

        return false;
    }
}
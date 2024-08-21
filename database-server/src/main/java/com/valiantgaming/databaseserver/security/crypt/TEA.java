package com.valiantgaming.databaseserver.security.crypt;

import com.valiantgaming.databaseserver.utility.Utility;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.security.SecureRandom;
import java.util.Arrays;

// TODO: Update code to remove unnecessary methods and do code clean up.
@Log4j2
public class TEA
{
    private static byte[] key;
    private static byte[] passMask;
    private static byte[] result;
    private static int sum;
    private static final int delta = 0x9e3779b9;

    // TODO: Update passMask and result to reflect the new password length. Length: password + 1
    @SneakyThrows
    public static byte[] encryptPassword(byte[] inKey, byte[] inPassword)
    {
        passMask = new byte[24];
        key = new byte[4];
        result = new byte[24];

        // The max number is 23 because there is one separator byte between password and filler.
        byte[] filler = new byte[23 - inPassword.length];
        // Securely randomize the bytes to create a unique salt.
        SecureRandom.getInstance("SHA1PRNG").nextBytes(filler);

        // Copy the passInput to passMask.
        System.arraycopy(inPassword, 0, passMask, 0, inPassword.length);
        // Copy the filler to the end of passMask.
        System.arraycopy(filler, 0, passMask, inPassword.length + 1, filler.length);

        int keyValue = Utility.byteArrayToInt(inKey);

        key[0] = (byte) keyValue;
        key[1] = (byte) (keyValue + 1);
        key[2] = (byte) (keyValue + 2);
        key[3] = (byte) (keyValue + 3);

        byte[] enc1 = encrypt(Arrays.copyOfRange(passMask, 0, 8), key);
        byte[] enc2 = encrypt(Arrays.copyOfRange(passMask, 8, 16), key);
        byte[] enc3 = encrypt(Arrays.copyOfRange(passMask, 16, 23), key);

        Utility.strncpy(enc1, result, 0);
        Utility.strncpy(enc2, result, 8);
        Utility.strncpy(enc3, result, 16);

        return result;
    }

    public static byte[] passwordDecode(byte[] passInput, byte[] keyInput)
    {
        result = new byte[24];
        int keyValue = Utility.byteArrayToInt(keyInput);

        key = new byte[4];
        key[0] = (byte) keyValue;
        key[1] = (byte) (keyValue + 1);
        key[2] = (byte) (keyValue + 2);
        key[3] = (byte) (keyValue + 3);

        byte[] dec1 = decrypt(passInput, key);
        byte[] dec2 = decrypt(Arrays.copyOfRange(passInput, 8, 16), key);
        byte[] dec3 = decrypt(Arrays.copyOfRange(passInput, 16, 23), key);

        Utility.strncpy(dec1, result, 0);
        Utility.strncpy(dec2, result, 8);
        Utility.strncpy(dec3, result, 16);

        return Utility.cutTail(result);
    }

    private static byte[] encrypt(byte[] src, byte[] key)
    {
        sum = 0;

        int v0 = Utility.byteArrayToInt(Arrays.copyOfRange(src, 0, 4));
        int v1 = Utility.byteArrayToInt(Arrays.copyOfRange(src, 4, 8));

        for (int i = 0; i < 32; i++)
        {
            sum += delta;
            v0 += ((v1 << 4) + key[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + key[1]);
            v1 += ((v0 << 4) + key[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + key[3]);
        }

        return Utility.intToByteArray(v0, v1);
    }

    private static byte[] decrypt(byte[] src, byte[] key)
    {
        sum = 0xc6ef3720;

        int v0 = Utility.byteArrayToInt(Arrays.copyOfRange(src, 0, 4));
        int v1 = Utility.byteArrayToInt(Arrays.copyOfRange(src, 4, 8));

        for (int i = 0; i < 32; i++)
        {
            v1 -= ((v0 << 4) + key[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + key[3]);
            v0 -= ((v1 << 4) + key[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + key[1]);
            sum -= delta;
        }

        return Utility.intToByteArray(v0, v1);

    }

    /*
        @author Ashima
        Generates a secure 4 byte key.
     */
    @SneakyThrows
    public static byte[] generateKey()
    {
        key = new byte[4];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(key);

        return key;
    }
}
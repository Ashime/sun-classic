package com.valiantgaming.databaseserver.security.crypt;

import com.valiantgaming.databaseserver.utility.Utility;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

@Log4j2
public class AES256
{
    private static final int keySize = 256; // 32 bytes
    private static final int ivLength = 12;
    private static final int tagLength = 128; // 16 bytes

    private static Cipher cipher;
    private static SecretKey secretKey;
    private static GCMParameterSpec gcmParameterSpec;

    private static final String encFileName = "./Config/DatabaseServer/key.enc";

    @SneakyThrows
    public AES256()
    {
        cipher = Cipher.getInstance("AES/GCM/NoPadding");
    }

    @SneakyThrows
    public SecretKey generateKey()
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keySize);

        return keyGenerator.generateKey();
    }

    @SneakyThrows
    public byte[] generateIV()
    {
        byte[] iv = new byte[ivLength];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(iv);

        return iv;
    }

    @SneakyThrows
    public void encryptFile(String key, String iv, byte[] message)
    {
        secretKey = new SecretKeySpec(Utility.hexStringToByteArray(key), "AES");
        gcmParameterSpec = new GCMParameterSpec(tagLength, Utility.hexStringToByteArray(iv));
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] encMessage = cipher.doFinal(message);

        FileOutputStream fileOutStream = new FileOutputStream(encFileName);
        fileOutStream.write(encMessage);
        fileOutStream.close();
    }

    @SneakyThrows
    public String decryptFile(String key, String iv)
    {
        FileInputStream fileInStream = new FileInputStream(encFileName);
        byte[] encMessage = fileInStream.readAllBytes();
        fileInStream.close();

        secretKey = new SecretKeySpec(Utility.hexStringToByteArray(key), "AES");
        gcmParameterSpec = new GCMParameterSpec(tagLength, Utility.hexStringToByteArray(iv));
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        return Utility.byteArrayToHexString(cipher.doFinal(encMessage));
    }
}
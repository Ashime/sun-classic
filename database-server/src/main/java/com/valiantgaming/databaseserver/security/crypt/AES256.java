package com.valiantgaming.databaseserver.security.crypt;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@Log4j2
public class AES256
{
    private static final int keySize = 256;
    private static final int ivLength = 12;
    private static final int tagLength = 128;

    @Autowired
    public AES256()
    {

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
    public static void encryptFile(SecretKey key, byte[] iv, String fileName, byte[] message)
    {
        // TODO: Ensure the key and iv are stored in the database.

        // output: [file_name].enc
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

        // TODO: Put message in file if exists (otherwise create) and encrypt the file.
    }

    public static void decryptFile()
    {
        // output: [file_name].txt
    }
}
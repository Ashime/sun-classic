package com.valiantgaming.databaseserver.security.crypt;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

@Log4j2
public class RSA
{
    private static Cipher cipher;
    private static final int keySize = 2048;

    @SneakyThrows
    public RSA()
    {
        cipher = Cipher.getInstance("RSA");
    }

    @SneakyThrows
    public KeyPair generateKeyPair()
    {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);

        return generator.generateKeyPair();
    }

    @SneakyThrows
    public byte[] encrypt(PrivateKey privateKey, byte[] message)
    {
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return cipher.doFinal(message);
    }

    @SneakyThrows
    public byte[] decrypt(PrivateKey privateKey, byte[] encryptedMessage)
    {
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedMessage);
    }
}
package com.valiantgaming.commons.security.hash;

// https://blog.mozilla.org/security/2011/05/10/sha-512-w-per-user-salts-is-not-enough/

import com.valiantgaming.commons.utility.Utility;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Log4j2
@NoArgsConstructor
public class SHA
{
    private static SHA instance;

    @Getter
    private static final int hmacKeySize = 64;
    @Getter @Setter
    private static boolean recreateHmacKey;
    @Getter @Setter
    private static String hmacKey;

    @SneakyThrows
    public static byte[] generateHmacKey()
    {
        byte[] result = new byte[hmacKeySize];
        SecureRandom.getInstance("SHA1PRNG").nextBytes(result);

        return result;
    }

    @SneakyThrows
    public static byte[] getMac(String message)
    {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);

        return sha512Hmac.doFinal(Utility.hexStringToByteArray(message + hmacKey));
    }

    @SneakyThrows
    public static byte[] getHash(String message)
    {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        return messageDigest.digest(message.getBytes(StandardCharsets.UTF_8));
    }

    public static SHA getInstance()
    {
        if(instance == null)
            synchronized (SHA.class)
            {
                if(instance == null)
                    instance = new SHA();
            }

        return instance;
    }
}
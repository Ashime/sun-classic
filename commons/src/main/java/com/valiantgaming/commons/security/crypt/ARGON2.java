package com.valiantgaming.commons.security.crypt;

import com.valiantgaming.commons.security.hash.SHA;
import com.valiantgaming.commons.utility.Utility;
import lombok.Setter;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

// https://blog.mozilla.org/security/2011/05/10/sha-512-w-per-user-salts-is-not-enough/
// https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#argon2id
public class ARGON2
{
    private static final int saltLength = 16;
    private static final int hashLength = 32;

    @Setter
    private static int memoryKb = 19456;
    @Setter
    private static int iterations = 2;
    @Setter
    private static int parallelism = 1;

    private static Argon2PasswordEncoder encoder()
    {
        return new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memoryKb, iterations);
    }

    /**
     * SHA-512-hashes the password first so arbitrarily long passphrases are condensed to a
     * fixed-size input before Argon2 sees them, hex-encoded rather than passed as raw digest
     * bytes so no stray byte value (e.g. an embedded {@code 0x00}) can be misread by the
     * encoder. Argon2PasswordEncoder generates and embeds its own salt in the returned string,
     * so nothing else needs to track one separately.
     */
    public String hashPassword(String password)
    {
        String preHashed = Utility.byteArrayToHexString(SHA.getHash(password));

        return encoder().encode(preHashed);
    }

    public boolean validatePassword(String dbPassword, String inPassword)
    {
        String preHashed = Utility.byteArrayToHexString(SHA.getHash(inPassword));

        return encoder().matches(preHashed, dbPassword);
    }
}

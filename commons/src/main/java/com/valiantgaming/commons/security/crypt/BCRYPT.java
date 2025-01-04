package com.valiantgaming.commons.security.crypt;

import com.valiantgaming.commons.security.hash.SHA;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCrypt;

// https://blog.mozilla.org/security/2011/05/10/sha-512-w-per-user-salts-is-not-enough/
public class BCRYPT
{
    // TODO: Need a method (here or another class) to handle passwords with outdated log rounds.
    // TODO: Need a method to handle checking old and new passwords are not the same.

    @Setter
    private static int logRounds;

    public void hashPassword(String password)
    {
        String salt = BCrypt.gensalt(logRounds);
        BCrypt.hashpw(SHA.getHash(password), salt);
    }
}

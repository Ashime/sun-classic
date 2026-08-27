package com.valiantgaming.commons.security.crypt;

import com.valiantgaming.commons.utility.Utility;
import lombok.SneakyThrows;

import java.security.SecureRandom;

/**
 * TEA (Tiny Encryption Algorithm), as used to encrypt the password field in
 * {@code U2A_askAuthUser}. Ported from an older working implementation
 * ({@code Ashime/LoginServer}, itself based on {@code CwaniX/OpenSUN-Emu}) that was
 * confirmed to interoperate with a real SUN client, rather than a textbook 128-bit-key port.
 *
 * <p><b>Not standard 128-bit-key TEA.</b> The key handed to the client (in
 * {@code A2U_ansReady}/{@code A2L_ansReady}) is just 4 bytes, read as a single {@code int}
 * and expanded into the four round-key slots as {@code keyValue}, {@code keyValue+1},
 * {@code keyValue+2}, {@code keyValue+3} - each truncated to a single byte. That's a much
 * smaller keyspace than real TEA (four independent 32-bit words), so treat this as
 * obfuscation matching the client's protocol, not real cryptographic security.
 *
 * <p>The round function relies on Java's default (signed) byte-to-int promotion when adding
 * a {@code key[n]} byte - do not mask with {@code & 0xFF}, that would change the output and
 * break compatibility with the client.
 */
public class TEA
{
    private static final int DELTA = 0x9e3779b9;
    private static final int DECODE_SUM_INIT = 0xc6ef3720;
    private static final int ROUNDS = 32;

    /**
     * Encrypts a password for the wire: copies it (truncated to 12 bytes, the field's
     * plaintext capacity) into a 16-byte null-padded buffer and TEA-encrypts it as two
     * consecutive 8-byte blocks.
     */
    public static byte[] passwordEncode(String password, byte[] keyInput)
    {
        byte[] key = expandKey(keyInput);

        byte[] passMask = new byte[16];
        byte[] passBytes = password.getBytes();
        System.arraycopy(passBytes, 0, passMask, 0, Math.min(passBytes.length, 12));

        byte[] block1 = encode(Utility.split(passMask, 0, 8), key);
        byte[] block2 = encode(Utility.split(passMask, 8, 16), key);

        byte[] result = new byte[16];
        System.arraycopy(block1, 0, result, 0, 8);
        System.arraycopy(block2, 0, result, 8, 8);

        return result;
    }

    /** Decrypts a password field from the wire, trimming trailing null padding. */
    public static byte[] passwordDecode(byte[] passInput, byte[] keyInput)
    {
        byte[] key = expandKey(keyInput);

        byte[] block1 = decode(Utility.split(passInput, 0, 8), key);
        byte[] block2 = decode(Utility.split(passInput, 8, 16), key);

        byte[] result = new byte[16];
        System.arraycopy(block1, 0, result, 0, 8);
        System.arraycopy(block2, 0, result, 8, 8);

        return Utility.cutTail(result);
    }

    /** Encrypts one 8-byte block ({@code src[0:4]} = v0, {@code src[4:8]} = v1). */
    public static byte[] encode(byte[] src, byte[] key)
    {
        int v0 = Utility.byteArrayToInt(Utility.split(src, 0, 4));
        int v1 = Utility.byteArrayToInt(Utility.split(src, 4, 8));
        int sum = 0;

        for(int i = 0; i < ROUNDS; i++)
        {
            sum += DELTA;
            v0 += ((v1 << 4) + key[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + key[1]);
            v1 += ((v0 << 4) + key[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + key[3]);
        }

        return Utility.intToByteArray(v0, v1);
    }

    /** Decrypts one 8-byte block. */
    public static byte[] decode(byte[] src, byte[] key)
    {
        int v0 = Utility.byteArrayToInt(Utility.split(src, 0, 4));
        int v1 = Utility.byteArrayToInt(Utility.split(src, 4, 8));
        int sum = DECODE_SUM_INIT;

        for(int i = 0; i < ROUNDS; i++)
        {
            v1 -= ((v0 << 4) + key[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + key[3]);
            v0 -= ((v1 << 4) + key[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + key[1]);
            sum -= DELTA;
        }

        return Utility.intToByteArray(v0, v1);
    }

    /**
     * Expands the 4-byte key handed out on the wire into the four single-byte round-key
     * slots the round function uses - see this class's comment for why these are bytes,
     * not 32-bit words.
     */
    private static byte[] expandKey(byte[] keyInput)
    {
        int keyValue = Utility.byteArrayToInt(keyInput);

        return new byte[] {
                (byte) keyValue,
                (byte) (keyValue + 1),
                (byte) (keyValue + 2),
                (byte) (keyValue + 3)
        };
    }

    /** Generates the 4-byte key to hand out in {@code A2U_ansReady}/{@code A2L_ansReady}. */
    @SneakyThrows
    public static byte[] generateKey()
    {
        byte[] key = new byte[4];
        SecureRandom.getInstanceStrong().nextBytes(key);
        key[0] = 0x00; // preserved from the reference implementation - reason unclear

        return key;
    }
}

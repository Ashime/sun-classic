package com.valiantgaming.commons.utility;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.codec.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Arrays;

@Log4j2
public class Utility
{
    public static String ASCIIToHexString(String input)
    {
        StringBuilder sb = new StringBuilder();

        for(char ch : input.toCharArray())
        {
            sb.append(String.format("%02x", (int) ch));
        }

        return sb.toString().toUpperCase();
    }

    public static String byteArrayToHexString(byte[] input)
    {
        return new String(Hex.encode(input)).toUpperCase();
    }

    public static short byteArrayToShort(byte[] input)
    {
        return ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    /**
     * @param input  - The byte[] that you wish to flip some bytes in.
     * @param index1 - The index of the byte you wish to flip.
     * @param index2 - The index of the second byte you wish to flip.
     */
    public static void flip(byte[] input, int index1, int index2)
    {
        byte b1 = input[index1];
        byte b2 = input[index2];

        input[index1] = b2;
        input[index2] = b1;
    }

    public static String hexStringToASCII(String input)
    {
        StringBuilder output = new StringBuilder();

        for(int i = 0; i < input.length(); i += 2)
        {
            String str = input.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

    public static byte[] hexStringToByteArray(String input)
    {
        return Hex.decode(input);
    }

    public static byte[] intToByteArray(short input)
    {
        return ByteBuffer.allocate(2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(input)
                .array();
    }

    public static byte[] longToBytes(long input)
    {
        return ByteBuffer.allocate(Long.BYTES).putLong(input).array();
    }

    public static String requireEnv(String name)
    {
        String value = System.getenv(name);

        if (value == null || value.isBlank())
        {
            log.fatal("Missing required environment variable: {}", name);
            System.exit(1);
        }

        return value;
    }

    /**
     * @param input  - The byte[] that you wish to split.
     * @param index1 - Starting index.
     * @param index2 - Ending index.
     * @return - Returns a byte[] that was split from the input.
     */
    public static byte[] split(byte[] input, int index1, int index2)
    {
        return Arrays.copyOfRange(input, index1, index2);
    }

    public static boolean verifyTimestamp(byte[] input, long timestampOffset)
    {
        long currentTime = Instant.now().toEpochMilli();

        ByteBuffer buffer = ByteBuffer.wrap(input);
        long timestamp = buffer.getLong();
        long timeDifference = Math.abs(currentTime - timestamp);

        if(timeDifference <= timestampOffset)
            return true;
        else
            return false;
    }

    public static byte toByte(byte input)
    {
        return Byte.parseByte(Integer.toHexString(input));
    }
}
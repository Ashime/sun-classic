package com.valiantgaming.commons.utility;

import org.springframework.security.crypto.codec.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Utility
{
    public static String byteArrayToHexString(byte[] input)
    {
        return new String(Hex.encode(input)).toUpperCase();
    }

    public static byte[] hexStringToByteArray(String input)
    {
        return Hex.decode(input);
    }

    public static void strncpy(byte[] input, byte[] output, int start)
    {
        for (int i = 0; i < input.length; i++)
        {
            output[i + start] = input[i];
        }
    }

    public static int byteArrayToInt(byte[] input)
    {
        return ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] intToByteArray(int... input)
    {
        ByteBuffer buffer = ByteBuffer.allocate(input.length * 4).order(ByteOrder.LITTLE_ENDIAN);

        for (int i : input)
        {
            buffer.putInt(i);
        }

        return buffer.array();
    }

    /**
     * @param input  - The byte[] that you wish to flip some bytes in.
     * @param index1 - The index of the byte you wish to flip.
     * @param index2 - The index of the second byte you wish to flip.
     * @return - Returns a byte[] of the input with flipped bytes.
     */
    public static byte[] flip(byte[] input, int index1, int index2)
    {
        byte[] output = Arrays.copyOfRange(input, 0, input.length);

        byte b1 = input[index1];
        byte b2 = input[index2];

        output[index1] = b2;
        output[index2] = b1;

        return output;
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
}
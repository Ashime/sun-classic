package com.valiantgaming.databaseserver.utility;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utility
{
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

    public static byte[] cutTail(byte[] input)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int counter = 0;

        while (counter < input.length)
        {
            if (input[counter] != 0x00)
            {
                out.write(input[counter]);
                counter++;
            }
            else {
                break;
            }
        }

        return out.toByteArray();
    }
}
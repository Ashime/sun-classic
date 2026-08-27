package com.valiantgaming.commons.network.packet;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteOrder;

/**
 * Every module's Netty pipeline frames packets the same way: a 2-byte <b>little-endian</b>
 * length (covering everything after the length field itself - category, protocol, and data)
 * built by each module's own {@code PacketEncoder}/{@code Encoder}.
 *
 * <p>Little-endian is what the real game client speaks, confirmed two ways: the captured
 * {@code A2L_ansVerifyVersion} packet in {@code Protocol} shows {@code Packet Size: 0e 00} for
 * a 14-byte message, and a live client answers {@code A2U_ansReady} only when its header is
 * sent little-endian (it replies {@code 25 00 33 01 ...}, i.e. 37 bytes, same convention).
 * The coders previously byte-swapped this into big-endian on both sides, which was
 * self-consistent between our own servers but left the real client waiting on a length it read
 * as 0x0600 until it timed out.
 *
 * <p>Every module's hand-rolled {@code byte[] -> byte[]} decoder (the one immediately after
 * {@code ByteArrayDecoder} in each pipeline) assumes one Netty {@code channelRead} event is
 * exactly one of these frames - true only by luck of TCP segment timing. Nothing upstream of
 * it ever reassembled a frame split across reads or split a read that happened to coalesce
 * several frames together, so either case corrupted or dropped packets. This factory produces
 * the frame decoder that has to sit between {@code ByteArrayDecoder} and the raw {@code
 * ByteBuf} source (i.e. added to the pipeline just before {@code ByteArrayDecoder}) to
 * guarantee that invariant - {@code initialBytesToStrip} is 0 so the emitted frame still
 * starts with the 2-byte length header, matching what every existing decoder already expects
 * and manually strips itself.
 */
public final class PacketFraming
{
    /** Generously above any packet this protocol actually sends; just a sanity ceiling against
     * a corrupt/hostile length field, not a real observed maximum. */
    private static final int MAX_FRAME_LENGTH = 65535;

    private static final int LENGTH_FIELD_OFFSET = 0;
    private static final int LENGTH_FIELD_LENGTH = 2;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private PacketFraming()
    {
    }

    public static LengthFieldBasedFrameDecoder newFrameDecoder()
    {
        // Netty's frame decoder defaults to big-endian, so the byte order has to be stated
        // explicitly here or it disagrees with the header the encoders write.
        return new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN,
                MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP, true);
    }
}

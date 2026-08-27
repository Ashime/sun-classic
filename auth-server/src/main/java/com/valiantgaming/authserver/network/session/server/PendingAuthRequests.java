package com.valiantgaming.authserver.network.session.server;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Correlates an outstanding {@code S2S_askAuthUser} request with the game-client channel that
 * triggered it. The S2S connection to the Database Server is a single shared, persistent
 * channel, while multiple game clients can be mid-login concurrently on the C2S side, so the
 * {@code S2S_ansAuthUser} reply needs a request ID to find its way back to the right client.
 */
public class PendingAuthRequests
{
    public record PendingAuthRequest(ChannelHandlerContext ctx, String username) {}

    private static final AtomicInteger sequence = new AtomicInteger();
    private static final Map<Integer, PendingAuthRequest> pending = new ConcurrentHashMap<>();

    public static int register(ChannelHandlerContext ctx, String username)
    {
        int requestId = sequence.incrementAndGet();
        pending.put(requestId, new PendingAuthRequest(ctx, username));

        return requestId;
    }

    public static PendingAuthRequest resolve(int requestId)
    {
        return pending.remove(requestId);
    }
}

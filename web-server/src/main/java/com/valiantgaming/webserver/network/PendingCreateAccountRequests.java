package com.valiantgaming.webserver.network;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Correlates an outstanding {@code S2S_askCreateAccount} request with the HTTP request thread
 * that triggered it. The S2S connection to the Database Server is a single shared, persistent
 * channel, while multiple registration requests can be in flight concurrently on the REST side,
 * so the {@code S2S_ansCreateAccount} reply needs a request ID to find its way back to the right
 * caller - resolved here as a {@link CompletableFuture} the controller thread blocks on, since
 * (unlike a game client's persistent channel) an HTTP request/response cycle has no long-lived
 * connection to write the answer back onto later.
 */
public class PendingCreateAccountRequests
{
    private static final AtomicInteger sequence = new AtomicInteger();
    private static final Map<Integer, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    public static int register(CompletableFuture<String> future)
    {
        int requestId = sequence.incrementAndGet();
        pending.put(requestId, future);

        return requestId;
    }

    public static void resolve(int requestId, String message)
    {
        CompletableFuture<String> future = pending.remove(requestId);

        if(future != null)
        {
            future.complete(message);
        }
    }

    /** Drops a request that timed out on the caller's side, so it doesn't sit in the map forever
     * if {@code S2S_ansCreateAccount} never arrives. */
    public static void cancel(int requestId)
    {
        pending.remove(requestId);
    }
}

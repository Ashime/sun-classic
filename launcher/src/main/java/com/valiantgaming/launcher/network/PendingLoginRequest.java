package com.valiantgaming.launcher.network;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Correlates the launcher's single outstanding {@code U2A_askAuthUser} attempt with its
 * {@code A2U_ansAuthUser} reply. Unlike web-server's {@code PendingCreateAccountRequests}
 * (many concurrent HTTP callers, needs a request ID), the launcher only ever has one
 * connection to AuthServer and therefore at most one login attempt in flight at a time, so a
 * single slot is enough.
 */
public class PendingLoginRequest
{
    private static final AtomicReference<CompletableFuture<Boolean>> pending = new AtomicReference<>();

    public static CompletableFuture<Boolean> register()
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pending.set(future);

        return future;
    }

    public static void resolve(boolean authenticated)
    {
        CompletableFuture<Boolean> future = pending.getAndSet(null);

        if(future != null)
            future.complete(authenticated);
    }
}

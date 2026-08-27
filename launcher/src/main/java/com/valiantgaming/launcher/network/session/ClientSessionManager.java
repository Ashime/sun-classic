package com.valiantgaming.launcher.network.session;

import com.valiantgaming.commons.network.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Tracks the launcher's one-and-only connection to AuthServer.
 *
 * <p>Unlike {@code ServerSessionManager} in auth-server/database-server (which holds a list
 * keyed by remote address, since those servers juggle many peer connections), the launcher
 * only ever has a single outbound {@link ClientSession} at a time, so this just wraps one
 * nullable field instead of a collection.
 *
 * <p>Both fields are {@code volatile} because this manager is genuinely shared across threads:
 * {@code session} is written on the Netty event-loop thread (channelActive/channelInactive) and
 * read on the JavaFX Application Thread ({@code LoginController}), and {@code instance} would
 * otherwise be unsafe double-checked locking - without the volatile read, a second thread can
 * observe a non-null but not-yet-constructed instance, or miss it entirely and build a second
 * one whose {@code session} is null.
 */
public class ClientSessionManager extends SessionManager
{
    private static volatile ClientSessionManager instance;
    private volatile ClientSession session;

    @Override
    public void addSession(Object object)
    {
        ClientSession newSession = new ClientSession();
        newSession.setCtx((ChannelHandlerContext) object);
        session = newSession;
    }

    @Override
    public ClientSession getSession(Object object)
    {
        return session;
    }

    /** Convenience overload for callers off the Netty pipeline (e.g. {@code LoginController}
     * on the FX thread) that have no {@code ChannelHandlerContext} to pass. */
    public ClientSession getSession()
    {
        return session;
    }

    @Override
    public void updateSession(Object object)
    {
        session = (ClientSession) object;
    }

    @Override
    public void removeSession(Object object)
    {
        session = null;
    }

    @Override
    public void clearSessions()
    {
        session = null;
    }

    public static ClientSessionManager getInstance()
    {
        if(instance == null)
            synchronized (ClientSessionManager.class)
            {
                if(instance == null)
                    instance = new ClientSessionManager();
            }

        return instance;
    }
}

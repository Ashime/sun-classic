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
 */
public class ClientSessionManager extends SessionManager
{
    private static ClientSessionManager instance;
    private ClientSession session;

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

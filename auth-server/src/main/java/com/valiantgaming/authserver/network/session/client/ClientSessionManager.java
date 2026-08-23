package com.valiantgaming.authserver.network.session.client;

import com.valiantgaming.commons.network.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;

/**
 * Tracks one {@link ClientSession} per connected game client, keyed by channel identity
 * (unlike {@code ServerSessionManager}, which matches S2S peers by IP since those sessions
 * exist before the connection is made - a client session is only ever created from
 * {@code channelActive}, so the {@link ChannelHandlerContext} itself is a stable, unique key
 * for as long as the channel is open).
 */
public class ClientSessionManager extends SessionManager
{
    private static ClientSessionManager instance;
    private final ArrayList<ClientSession> clientSessions = new ArrayList<>();

    @Override
    public void addSession(Object object)
    {
        ClientSession session = new ClientSession();
        session.setCtx((ChannelHandlerContext) object);

        clientSessions.add(session);
    }

    @Override
    public ClientSession getSession(Object object)
    {
        ChannelHandlerContext ctx = (ChannelHandlerContext) object;

        for(ClientSession s : clientSessions)
        {
            if(s.getCtx() == ctx)
            {
                return s;
            }
        }

        return null;
    }

    @Override
    public void updateSession(Object object)
    {
        ClientSession session = (ClientSession) object;
        for(ClientSession s : clientSessions)
        {
            if(s.getCtx() == session.getCtx())
            {
                if(clientSessions.remove(s))
                {
                    clientSessions.add(session);
                }
            }
        }
    }

    @Override
    public void removeSession(Object object)
    {
        ChannelHandlerContext ctx = (ChannelHandlerContext) object;
        clientSessions.removeIf(s -> s.getCtx() == ctx);
    }

    @Override
    public void clearSessions()
    {
        clientSessions.clear();
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

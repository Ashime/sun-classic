package com.valiantgaming.webserver.network.session.server;

import com.valiantgaming.webserver.database.entity.server.ServerInfo;
import com.valiantgaming.commons.network.session.SessionManager;
import io.netty.channel.Channel;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.ArrayList;

@Log4j2
public class ServerSessionManager extends SessionManager
{
    private static ServerSessionManager instance;
    private final ArrayList<ServerSession> serverSessions = new ArrayList<>();

    /** The single outbound S2S channel to the Database Server, set once {@code NioServer.initS2S()} connects. */
    private static Channel channel;

    public static void setChannel(Channel channel)
    {
        ServerSessionManager.channel = channel;
    }

    public static Channel getChannel()
    {
        return channel;
    }

    @Override
    public void addSession(Object object)
    {
        ServerInfo serverInfo = (ServerInfo) object;

        ServerSession session = new ServerSession();
        session.setServerInfo(serverInfo);

        serverSessions.add(session);
    }

    @Override
    public ServerSession getSession(Object object)
    {
        InetSocketAddress ipAddress = (InetSocketAddress) object;
        String[] address = ipAddress.toString().replace("/", "").split(":");

        for(ServerSession s : serverSessions)
        {
            if(s.getServerInfo().getPublicIP().equals(address[0]) || s.getServerInfo().getIpv4().equals(address[0])
                || s.getServerInfo().getLocalIP().equals(address[0]))
            {
                return s;
            }
        }

        return null;
    }

    @Override
    public void updateSession(Object object)
    {
        // removeIf rather than remove-inside-a-for-each: mutating the list while iterating it
        // throws ConcurrentModificationException on the following iteration.
        ServerSession session = (ServerSession) object;

        if(serverSessions.removeIf(s -> s.getServerInfo().equals(session.getServerInfo())))
        {
            serverSessions.add(session);
        }
    }

    @Override
    public void removeSession(Object object)
    {
        ServerSession session = (ServerSession) object;
        serverSessions.removeIf(s -> s.getServerInfo().equals(session.getServerInfo()));
    }

    @Override
    public void clearSessions()
    {
        serverSessions.clear();
    }

    public static ServerSessionManager getInstance()
    {
        if(instance == null)
            synchronized (ServerSessionManager.class)
            {
                if(instance == null)
                    instance = new ServerSessionManager();
            }

        return instance;
    }
}

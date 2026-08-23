package com.valiantgaming.authserver.network.session.server;

import com.valiantgaming.authserver.database.entity.server.ServerInfo;
import com.valiantgaming.authserver.security.crypt.AES;
import com.valiantgaming.authserver.security.crypt.RSA;
import com.valiantgaming.commons.network.session.SessionManager;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.ArrayList;

@Log4j2
public class ServerSessionManager extends SessionManager
{
    private static ServerSessionManager instance;
    private final ArrayList<ServerSession> serverSessions = new ArrayList<>();
    private static final RSA rsa = new RSA();
    private static final AES aes = new AES();

    @Override
    public void addSession(Object object)
    {
        ServerInfo serverInfo = (ServerInfo) object;

        ServerSession session = new ServerSession();
        session.setServerInfo(serverInfo);
        session.setRsaPublicKey(rsa.generateKeyPair().getPublic());
        session.setAesSecretKey(aes.generateKey());
        session.setAesIv(aes.generateIV());

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
        ServerSession session = (ServerSession) object;
        for(ServerSession s : serverSessions)
        {
            if(s.getServerInfo().equals(session.getServerInfo()))
            {
                if(serverSessions.remove(s))
                {
                    serverSessions.add(session);
                }
            }
        }
    }

    @Override
    public void removeSession(Object object)
    {
        ServerSession session = (ServerSession) object;
        for(ServerSession s : serverSessions)
        {
            if(s.getServerInfo().equals(session.getServerInfo()))
            {
                serverSessions.remove(s);
            }
        }
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
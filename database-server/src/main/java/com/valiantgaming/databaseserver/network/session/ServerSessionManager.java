package com.valiantgaming.databaseserver.network.session;

import com.valiantgaming.commons.network.session.ServerSession;
import com.valiantgaming.commons.network.session.SessionManager;
import com.valiantgaming.commons.security.crypt.AES;
import com.valiantgaming.commons.security.crypt.RSA;
import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.security.KeyPair;
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
        KeyPair rsaKeyPair = rsa.generateKeyPair();

        ServerSession session = new ServerSession();
        session.setServerInfo(serverInfo);
        session.setRsaPrivateKey(rsaKeyPair.getPrivate());
        session.setRsaPublicKey(rsaKeyPair.getPublic());
        session.setAesSecretKey(aes.generateKey());
        session.setAesIv(aes.generateIV());

        serverSessions.add(session);
    }

    @Override
    public ServerSession getSession(Object object)
    {
        InetSocketAddress ipAddress = (InetSocketAddress) object;
        String[] address = ipAddress.toString().split(":");

        for(ServerSession s : serverSessions)
        {
            if(((ServerInfo)s.getServerInfo()).getPublicIP().equals(address[0]) || ((ServerInfo)s.getServerInfo()).getIpv4().equals(address[0])
                || ((ServerInfo)s.getServerInfo()).getLocalIP().equals(address[0]))
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
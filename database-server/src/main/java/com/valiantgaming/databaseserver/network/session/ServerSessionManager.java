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
        ServerSession session = null;
        InetSocketAddress ipAddress = (InetSocketAddress) object;
        String[] address = ipAddress.toString().replace("/", "").split(":");

        for(ServerSession s : serverSessions)
        {
            ServerInfo serverInfo = (ServerInfo) s.getServerInfo();

            if((serverInfo.isPublicEnabled() && serverInfo.getPublicIP().equals(address[0])) ||
                    (serverInfo.isIpv4Enabled() && serverInfo.getIpv4().equals(address[0])) ||
                    (serverInfo.isLocalEnabled() && serverInfo.getLocalIP().equals(address[0])))
            {
                session = s;
            }
        }

        return session;
    }

    @Override
    public void updateSession(Object object)
    {
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
package com.valiantgaming.authserver.network.firewall.server;

import com.valiantgaming.authserver.config.AuthServerConfig;
import com.valiantgaming.authserver.database.entity.ServerInfo;
import com.valiantgaming.authserver.network.session.ServerSessionManager;
import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class IpRules implements IpFilterRule
{
    private static IpRules instance;

    @Setter
    private static List<ServerInfo> serverInfoList;

    @Override
    public boolean matches(InetSocketAddress remoteAddress)
    {
        String[] address = remoteAddress.toString().replace("/", "").split(":");
        boolean isAccepted = false;

        if(serverInfoList == null || serverInfoList.isEmpty())
        {
            serverInfoList = new ArrayList<>();

            if(AuthServerConfig.getDbServerIp().equals(address[0]) && String.valueOf(AuthServerConfig.getDbServerPort()).equals(address[1]))
            {
                // Temp Data. Will be replaced further along in the pipeline.
                ServerInfo serverInfo = new ServerInfo();
                serverInfo.setServerID(0);
                serverInfo.setServerName("DATABASE SERVER");
                serverInfo.setPublicIP(address[0]);
                serverInfo.setPublicPort(address[1]);
                serverInfo.setIpv4(address[0]);
                serverInfo.setIpv4Port(address[1]);
                serverInfo.setLocalIP(address[0]);
                serverInfo.setLocalPort(address[1]);
                serverInfo.setPublicEnabled(true);
                serverInfo.setIpv4Enabled(true);
                serverInfo.setLocalEnabled(true);
                serverInfo.setCreateDate(null);
                serverInfo.setModifiedDate(null);

                serverInfoList.add(serverInfo);
                ServerSessionManager.getInstance().addSession(serverInfo);
                isAccepted = true;
            }
        }
        else
        {
            for(ServerInfo s : serverInfoList)
            {
                if (s.isPublicEnabled() && (s.getPublicIP().equals(address[0]) && s.getPublicPort().equals(address[1])))
                {
                    ServerSessionManager.getInstance().addSession(s);
                    isAccepted = true;
                }
                else if (s.isIpv4Enabled() && (s.getIpv4().equals(address[0]) && s.getIpv4Port().equals(address[1])))
                {
                    ServerSessionManager.getInstance().addSession(s);
                    isAccepted = true;
                }
                else if (s.isLocalEnabled() && (s.getLocalIP().equals(address[0]) && s.getLocalPort().equals(address[1])))
                {
                    ServerSessionManager.getInstance().addSession(s);
                    isAccepted = true;
                }
            }
        }

        return isAccepted;
    }

    @Override
    public IpFilterRuleType ruleType()
    {
        return IpFilterRuleType.ACCEPT;
    }

    public static IpRules getInstance()
    {
        if(instance == null)
            synchronized (IpRules.class)
            {
                if(instance == null)
                    instance = new IpRules();
            }

        return instance;
    }
}
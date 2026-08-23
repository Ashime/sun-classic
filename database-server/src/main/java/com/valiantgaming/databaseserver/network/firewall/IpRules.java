package com.valiantgaming.databaseserver.network.firewall;

import com.valiantgaming.databaseserver.database.entity.server.ServerInfo;
import com.valiantgaming.databaseserver.network.session.ServerSessionManager;
import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.IpFilterRuleType;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;
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

        assert serverInfoList != null;
        for(ServerInfo s : serverInfoList)
        {
            if(s.isPublicEnabled() && (s.getPublicIP().equals(address[0]) && s.getPublicPort().equals(address[1])))
            {
                ServerSessionManager.getInstance().addSession(s);
                isAccepted = true;
            }
            else if(s.isIpv4Enabled() && (s.getIpv4().equals(address[0]) && s.getIpv4Port().equals(address[1])))
            {
                ServerSessionManager.getInstance().addSession(s);
                isAccepted = true;
            }
            else if(s.isLocalEnabled() && (s.getLocalIP().equals(address[0]) && s.getLocalPort().equals(address[1])))
            {
                ServerSessionManager.getInstance().addSession(s);
                isAccepted = true;
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
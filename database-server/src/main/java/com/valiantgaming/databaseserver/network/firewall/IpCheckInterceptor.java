package com.valiantgaming.databaseserver.network.firewall;

import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorSupport;
import org.springframework.integration.ip.tcp.connection.TcpSender;

public class IpCheckInterceptor extends TcpConnectionInterceptorSupport
{
    @Override
    public void registerSender(TcpSender sender)
    {
        super.registerSender(sender);
    }
}
package com.valiantgaming.databaseserver.network.firewall;

import lombok.extern.log4j.Log4j2;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorSupport;
import org.springframework.integration.ip.tcp.connection.TcpSender;

@Log4j2
public class IpCheckInterceptor extends TcpConnectionInterceptorSupport
{
    @Override
    public void registerSender(TcpSender sender)
    {
        super.registerSender(sender);
    }
}
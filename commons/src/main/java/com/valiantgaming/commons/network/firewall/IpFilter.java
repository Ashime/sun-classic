package com.valiantgaming.commons.network.firewall;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.IpFilterRule;
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.net.InetSocketAddress;

@Log4j2
public class IpFilter extends RuleBasedIpFilter
{
    public IpFilter(boolean acceptIfNotFound, IpFilterRule rules)
    {
        super(acceptIfNotFound, rules);
    }
    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception
    {
        log.info("Successful connection from " + remoteAddress.toString().replace("/", ""));
        return super.accept(ctx, remoteAddress);
    }

    @Override
    @SneakyThrows
    protected ChannelFuture channelRejected(ChannelHandlerContext ctx, InetSocketAddress remoteAddress)
    {
        log.warn("Closed unknown connection! Remote Address: " + remoteAddress.toString().replace("/", ""));
        return ctx.close().sync();
    }
}
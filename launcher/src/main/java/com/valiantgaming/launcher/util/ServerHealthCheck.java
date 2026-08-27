package com.valiantgaming.launcher.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A raw TCP connect/disconnect probe, independent of {@link com.valiantgaming.launcher.server.NioClient}'s
 * single persistent handshake connection - that connection is only ever attempted once at
 * launcher startup and never retried, so it can't answer "is the server reachable right now"
 * on an ongoing basis the way a status indicator needs.
 */
public final class ServerHealthCheck
{
    private ServerHealthCheck()
    {
    }

    public static boolean isReachable(String host, int port, int timeoutMillis)
    {
        try(Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return true;
        }
        catch(IOException e)
        {
            return false;
        }
    }
}

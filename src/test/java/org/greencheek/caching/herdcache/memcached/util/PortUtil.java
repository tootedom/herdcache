package org.greencheek.caching.herdcache.memcached.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Created by dominictootell on 20/04/2014.
 */
public class PortUtil {
    public static ServerSocket findFreePort() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(0,1000, InetAddress.getLoopbackAddress());
            server.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return server;
    }

    public static int getPort(ServerSocket server) {
        int port = server.getLocalPort();
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }

}

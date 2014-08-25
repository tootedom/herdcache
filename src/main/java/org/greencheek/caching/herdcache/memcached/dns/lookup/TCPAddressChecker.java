package org.greencheek.caching.herdcache.memcached.dns.lookup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by dominictootell on 29/03/2014.
 */
public class TCPAddressChecker implements AddressChecker {

    private final long connectionTimeoutInMillis;

    public TCPAddressChecker(long connectionTimeoutInMillis) {
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
    }

    @Override
    public boolean isAvailable(InetSocketAddress address) {
        SocketChannel ch = null;
        Selector selector = null;

        boolean connectedOk = false;
        boolean hasException = false;

        try {
            // Create selector
            selector = Selector.open();

            ch = SocketChannel.open();
            ch.configureBlocking(false);

            ch.connect(address);
            // Register connection event to selector (OP_CONNECT type)
            ch.register(selector, SelectionKey.OP_CONNECT);

            // Waiting for the connection
            while (selector.select(connectionTimeoutInMillis) > 0) {
                // Get keys
                Set keys = selector.selectedKeys();
                Iterator i = keys.iterator();

                // For each key...
                while (i.hasNext()) {
                    SelectionKey key = (SelectionKey)i.next();

                    // Remove the current key
                    i.remove();

                    // Attempt a connection
                    if (key.isConnectable()) {
                        // Get the socket channel held by the key
                        SocketChannel channel = (SocketChannel)key.channel();
                        // Close pendent connections
                        if(channel.finishConnect()) {
                            // Remove the interrest in the connect
                            // as you are connected
                            key.interestOps(key.interestOps() ^ SelectionKey.OP_CONNECT);
                        }
                    }

                    connectedOk = true;
                }

                break;
            }
        } catch (Exception e) {
            hasException = true;

        } finally {
            if(selector!=null) {
                try {
                    selector.close();
                } catch (Exception e) {

                }
            }
            if (ch != null) {
                try {
                    Socket s = ch.socket();
                    if (s != null) {
                        try {
                            s.setSoLinger(false, 0);
                            s.close();
                        } catch (IOException e) {
                        }
                    }
                    ch.close();
                } catch (IOException e) {
                }
            }
            if(connectedOk && !hasException) {
                return true;
            } else {
                return false;
            }
        }
    }
}

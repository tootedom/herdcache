/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.greencheek.caching.herdcache.memcached.elasticacheconfig.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.ConfigMessage;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.DelayConfigResponse;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.SendAllMessages;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

/**
 * Echoes back any received data from a client.
 */
public final class StringServer implements TestRule {

    private final String[] message;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private volatile int port;

    private final TimeUnit startDelayUnit;
    private final long startDelay;

    public StringServer(String msg) {
        this(msg,-1, TimeUnit.SECONDS);
    }

    public StringServer(String msg, long startDelay, TimeUnit startDelayUnit) {
        this.message = new String[]{msg};
        this.startDelay = startDelay;
        this.startDelayUnit = startDelayUnit;
    }

    public StringServer(String[] msg, long startDelay, TimeUnit startDelayUnit) {
        this.message = msg;
        this.startDelay = startDelay;
        this.startDelayUnit = startDelayUnit;
    }


    public int getPort() {
        return port;
    }

    /**
     * Override to set up your specific external resource.
     *
     * @throws if setup fails (which will disable {@code after}
     */
    public void before(final String[] message,
                          final TimeUnit delayUnit,
                          final long delay,
                          boolean sendAllMessages) {
        final ServerSocket socket = findFreePort();

        final ChannelHandler sharedHandler = new StringBasedServerHandler(message,delayUnit,delay,sendAllMessages);
        // do nothing
        try {
            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .option(ChannelOption.SO_SNDBUF,  100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast(new ConfigGetClusterDecoder());
                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(sharedHandler);
                        }
                    });


            // Start the server.
            if(startDelay>0) {
                port = getPortNoClose(socket);
                workerGroup.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            port = getPort(socket);
                            b.bind(port).sync();
                            outputStartedMessage();
                        } catch (InterruptedException e) {

                        }
                    }
                },startDelay,startDelayUnit);
            } else {
                port = getPort(socket);
                try {
                    b.bind(port).sync();
                    outputStartedMessage();
                } catch(InterruptedException e) {
                }
            }

        } finally {

        }
    }


    private void outputStartedMessage() {
        System.out.println("===========");
        System.out.println("ElastiCache Config Server started on port: " + port);
        System.out.println("===========");
        System.out.flush();
    }

    private void outputShutdownMessage() {
        System.out.println("===========");
        System.out.println("ElastiCache Config Server shutdown on port: " + port);
        System.out.println("===========");
        System.out.flush();
    }
    /**
     * Override to tear down your specific external resource.
     */
    public void after() {
        // do nothing
        // Shut down all event loops to terminate all threads.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        outputShutdownMessage();
    }

    private ServerSocket findFreePort()  {
        try {
            ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            server.setReuseAddress(true);
            return server;
        } catch(IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private int getPortNoClose(ServerSocket server) {
        return server.getLocalPort();
    }

    private int getPort(ServerSocket server) {
        int port = server.getLocalPort();
        try {
            server.close();
        } catch (IOException e) {

        }
        return port;
    }

    public Statement apply(final Statement base, final Description description) {
        final ConfigMessage message = description.getAnnotation(ConfigMessage.class);
        final DelayConfigResponse delayBy = description.getAnnotation(DelayConfigResponse.class);
        final SendAllMessages sendAll = description.getAnnotation(SendAllMessages.class);

        final boolean sendAllMessages = sendAll == null ? false : true;
        final TimeUnit delayUnit;
        final long delayedFor;
        final String[] messageToSend;
        if(message == null) {
            messageToSend = this.message;
        } else {
            String[] msg = message.message();

            if (msg == null || msg.length == 0) {
                messageToSend = this.message;
            } else {
                messageToSend = msg;
            }
        }

        if(delayBy == null) {
            delayUnit = TimeUnit.SECONDS;
            delayedFor = -1;
        } else {
            delayUnit = delayBy.delayedForTimeUnit();
            delayedFor = delayBy.delayFor();
        }

        return new Statement() {


            @Override
            public void evaluate() throws Throwable {
                before(messageToSend,delayUnit,delayedFor,sendAllMessages);
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }


}

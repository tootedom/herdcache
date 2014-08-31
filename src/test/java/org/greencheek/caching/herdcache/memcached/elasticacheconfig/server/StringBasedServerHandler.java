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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class StringBasedServerHandler extends SimpleChannelInboundHandler<String> {

    private final ByteBuf[] msg;
    private final TimeUnit delayUnit;
    private final long delay;
    private final AtomicInteger index = new AtomicInteger(0);
    private final boolean sendAll;

    public StringBasedServerHandler(String[] msg,TimeUnit unit,long delay, boolean sendAllMessages) {
        ByteBuf b;

        this.msg = new ByteBuf[msg.length];

        for(int i =0;i<msg.length;i++) {
            this.msg[i] = createMessage(msg[i]);
        }


        this.delayUnit = unit;
        this.delay = delay;
        this.sendAll = sendAllMessages;

    }

    private ByteBuf createMessage(String message) {
        try {
            return Unpooled.wrappedBuffer(message.getBytes("UTF-8"));
        } catch(UnsupportedEncodingException e) {
            return Unpooled.wrappedBuffer(message.getBytes());
        }
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, String msg) {

        if(sendAll) {
            sendAll(ctx);
        } else {
            sendOne(ctx);
        }

    }

    private void sendAll(final ChannelHandlerContext ctx) {
        long delayFor = delay;
        for(ByteBuf message : this.msg) {
            ByteBuf messageCopy = message.copy();
            String messageCopyStr = messageCopy.toString(Charset.forName("UTF-8"));
            if (messageCopyStr.contains("${REMOTE_ADDR}")) {
                messageCopyStr = messageCopyStr.replace("${REMOTE_ADDR}", ctx.channel().remoteAddress().toString());
            }

            final ByteBuf messageToSend = stringToByteBuf(messageCopyStr);

            if (delay < 1) {
                ctx.writeAndFlush(messageToSend);
            } else {
                ctx.channel().eventLoop().schedule(new Runnable() {
                    @Override
                    public void run() {
                        ctx.writeAndFlush(messageToSend);
                    }
                }, delayFor, delayUnit);
            }
            delayFor+=delay;
        }
    }

    private void sendOne(final ChannelHandlerContext ctx) {
        final ByteBuf message = this.msg[index.getAndIncrement()%this.msg.length];

        ByteBuf messageCopy = message.copy();
        String messageCopyStr = messageCopy.toString(Charset.forName("UTF-8"));
        if(messageCopyStr.contains("${REMOTE_ADDR}")) {
            messageCopyStr = messageCopyStr.replace("${REMOTE_ADDR}",ctx.channel().remoteAddress().toString());
        }

        final ByteBuf messageToSend = stringToByteBuf(messageCopyStr);

        if(delay<1) {
            ctx.writeAndFlush(messageToSend);
        }
        else {
            ctx.channel().eventLoop().schedule(new Runnable() {
                @Override
                public void run() {
                    ctx.writeAndFlush(messageToSend);
                }
            },delay,delayUnit);
        }
    }

    private ByteBuf stringToByteBuf(String string) {
        try {
            return Unpooled.wrappedBuffer(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return Unpooled.wrappedBuffer(string.getBytes());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}


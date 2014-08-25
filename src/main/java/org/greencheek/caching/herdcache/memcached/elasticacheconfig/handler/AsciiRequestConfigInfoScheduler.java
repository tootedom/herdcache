package org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class AsciiRequestConfigInfoScheduler implements RequestConfigInfoScheduler {

    public static final TimeUnit GET_CONFIG_POLLING_TIME_UNIT = TimeUnit.SECONDS;
    public static final long GET_CONFIG_POLLING_TIME = 60;
    public static final long GET_CONIFG_INITIAL_POLLING_DELAY = 0;

    private static final ByteBuf GET_CONFIG_COMMAND = Unpooled.wrappedBuffer("config get cluster\r\n".getBytes());


    private final TimeUnit pollingTimeUnit;
    private final long initialRequestDelay;
    private final long pollingFrequency;

    public AsciiRequestConfigInfoScheduler() {
        this(GET_CONFIG_POLLING_TIME_UNIT,
             GET_CONIFG_INITIAL_POLLING_DELAY,
             GET_CONFIG_POLLING_TIME);
    }

    public AsciiRequestConfigInfoScheduler(TimeUnit pollingTimeUnit, long initalDelay, long pollingFrequency) {
        this.pollingTimeUnit = pollingTimeUnit;
        this.initialRequestDelay = initalDelay;
        this.pollingFrequency = pollingFrequency;
    }

    @Override
    public void schedule(EventExecutor executor,ChannelHandlerContext ctx) {
        executor.scheduleAtFixedRate(new RepeatTask(ctx),initialRequestDelay, pollingFrequency, pollingTimeUnit);
    }

    private final class RepeatTask implements Runnable {
        private final ChannelHandlerContext ctx;

        public RepeatTask(ChannelHandlerContext ctx){
            this.ctx = ctx;
        }

        public void run() {
            if(ctx.channel().isActive()){

                ctx.writeAndFlush(GET_CONFIG_COMMAND.copy());
            }
        }
    }
}

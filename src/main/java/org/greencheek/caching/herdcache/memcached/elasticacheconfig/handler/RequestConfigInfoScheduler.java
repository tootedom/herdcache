package org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

/**
 * Created by dominictootell on 20/07/2014.
 */
public interface RequestConfigInfoScheduler {
    public void schedule(EventExecutor executor, ChannelHandlerContext ctx);
}

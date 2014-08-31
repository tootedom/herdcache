package org.greencheek.caching.herdcache.memcached.elasticacheconfig.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by dominictootell on 21/07/2014.
 */
public class ConfigGetClusterDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        if (buf.readableBytes() < 20) {
            return;
        }

        String getConfig = buf.toString(buf.readerIndex(), 20, Charset.forName("ASCII"));

        buf.readerIndex(20);

        if(getConfig.indexOf("config get cluster")>-1) {
            out.add(getConfig);
        }
    }
}

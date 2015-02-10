package org.greencheek.caching.herdcache.memcached.elasticacheconfig.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.ReferenceCountUtil;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ClusterConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

public class ConfigInfoDecoder extends ReplayingDecoder<ConfigInfoDecodingState> {

    private static final RuntimeException INVALID_VERSION = new InvalidConfigVersionException();
    private static final Logger log = LoggerFactory.getLogger(ConfigInfoDecoder.class);


    private final ClusterConfigurationBuilder configBuilder = new ClusterConfigurationBuilder();
    public ConfigInfoDecoder() {
        // Set the initial state.
        super(ConfigInfoDecodingState.HEADER);
    }

    private boolean readStringLine(ByteBuf byteBuf, ConfigInfoDecodingState currentState, ConfigInfoDecodingState stateToTransitionTo) {
        int eol = findEndOfLine(byteBuf);
        if(eol!=-1) {
            String line = readLine(eol, byteBuf).toString(Charset.forName("UTF-8"));
            log.debug("Config line read: {}",line);
            configBuilder.setValue(line, currentState);
            checkpoint(stateToTransitionTo);
            return true;
        } else {
            return false;
        }
    }



    private boolean readLongLine(final ChannelHandlerContext ctx,ByteBuf byteBuf, ConfigInfoDecodingState currentState,ConfigInfoDecodingState stateToTransitionTo) {
        int eol = findEndOfLine(byteBuf);
        if(eol!=-1) {
            String version = readLine(eol,byteBuf).toString(Charset.forName("UTF-8"));
            log.debug("Config line read: {}",version);
            long versionNumber = parseLong(version, Long.MIN_VALUE);
            if(versionNumber== Long.MIN_VALUE) {
                ctx.fireExceptionCaught(INVALID_VERSION);
            }

            configBuilder.setValue(versionNumber, currentState);
            checkpoint(stateToTransitionTo);
            return true;
        }
        else {
            return false;
        }
    }

    public static long parseLong(String s,long invalidValue)
            throws NumberFormatException
    {
        int radix = 10;
        if (s == null) {
            return invalidValue;
        }

        if (radix < Character.MIN_RADIX) {
           return invalidValue;
        }
        if (radix > Character.MAX_RADIX) {
            return invalidValue;
        }

        long result = 0;
        boolean negative = false;
        int i = 0, len = s.length();
        long limit = -Long.MAX_VALUE;
        long multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+') {
                    return invalidValue;
                }


                if (len == 1) // Cannot have lone "+" or "-"
                {
                    return invalidValue;
                }
                i++;
            }
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0) {
                    return invalidValue;
                }
                if (result < multmin) {
                    return invalidValue;
                }
                result *= radix;
                if (result < limit + digit) {
                    return invalidValue;
                }
                result -= digit;
            }
        } else {
            return invalidValue;
        }
        return negative ? result : -result;
    }

    protected void reset() {
        configBuilder.init();
        this.state(ConfigInfoDecodingState.HEADER);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> objects) throws Exception {
        switch (state()) {
            case HEADER :
                if(!readStringLine(byteBuf, ConfigInfoDecodingState.HEADER, ConfigInfoDecodingState.VERSION)) {
                    byteBuf.readBytes(Integer.MAX_VALUE);
                }
            case VERSION:
                if(!readLongLine(channelHandlerContext,byteBuf, ConfigInfoDecodingState.VERSION, ConfigInfoDecodingState.NODES)) {
                    byteBuf.readBytes(Integer.MAX_VALUE);
                }
            case NODES:
                if(!readStringLine(byteBuf, ConfigInfoDecodingState.NODES, ConfigInfoDecodingState.BLANK)) {
                    byteBuf.readBytes(Integer.MAX_VALUE);
                }
            case BLANK:
                if(!readStringLine(byteBuf, ConfigInfoDecodingState.BLANK, ConfigInfoDecodingState.END)) {
                    byteBuf.readBytes(Integer.MAX_VALUE);
                }
            case END:
                if(!readStringLine(byteBuf, ConfigInfoDecodingState.END, ConfigInfoDecodingState.HEADER)) {
                    byteBuf.readBytes(Integer.MAX_VALUE);
                }
                objects.add(configBuilder.build());
                reset();
                break;
            default:
                throw new Error("Unknown decoding state: ");
        }
    }


    private ByteBuf readLine(int eol,ByteBuf buffer) {
        ByteBuf frame = null;
        try {
            final int length = eol - buffer.readerIndex();
            final int delimLength = buffer.getByte(eol) == '\r' ? 2 : 1;
            frame = buffer.readSlice(length);
            buffer.skipBytes(delimLength);
            return frame.retain();
        } finally {
            if(frame!=null) {
                ReferenceCountUtil.release(frame);
            }
        }
    }

    /**
     * Returns the index in the buffer of the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private static int findEndOfLine(final ByteBuf buffer) {
        final int n = buffer.writerIndex();
        for (int i = buffer.readerIndex(); i < n; i ++) {
            final byte b = buffer.getByte(i);
            if (b == '\n') {
                return i;
            } else if (b == '\r' && i < n - 1 && buffer.getByte(i + 1) == '\n') {
                return i;  // \r\n
            }
        }
        return -1;  // Not found.
    }

}
/**
 * Copyright (C) 2006-2009 Dustin Sallings
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */

package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.transcoders.TranscoderUtils;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.metrics.NoOpMetricRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.Compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Transcoder that serializes and compresses objects.
 */
public class SerializingTranscoder extends BaseSerializingTranscoder implements
        Transcoder<Object> {

    private static final Logger logger = LoggerFactory.getLogger(SerializingTranscoder.class);

    // Max size of content stored in bytes
    public static final int MAX_CONTENT_SIZE_IN_BYTES = 50 * 1024 * 1024;

    // name of metric that records the number of bytes that have been requested for decoding (come from memcached)
    public static final String DECODED_BYTES_METRIC_NAME = "bytesdecoded";

    // name of the metric that records the number of bytes that are to be sent to memcached
    public static final String ENCODED_BYTES_METRIC_NAME = "bytesencoded";


    public static final MetricRecorder DEFAULT_METRIC_RECORDER = new NoOpMetricRecorder();
    // General flags
    static final int SERIALIZED = 1;
    static final int COMPRESSED = 2;

    // Special flags for specially handled types.
    private static final int SPECIAL_MASK = 0xff00;
    static final int SPECIAL_BOOLEAN = (1 << 8);
    static final int SPECIAL_INT = (2 << 8);
    static final int SPECIAL_LONG = (3 << 8);
    static final int SPECIAL_DATE = (4 << 8);
    static final int SPECIAL_BYTE = (5 << 8);
    static final int SPECIAL_FLOAT = (6 << 8);
    static final int SPECIAL_DOUBLE = (7 << 8);
    static final int SPECIAL_BYTEARRAY = (8 << 8);

    private final TranscoderUtils tu = new TranscoderUtils(true);

    private final MetricRecorder metricRecorder;

    /**
     * Get a serializing transcoder with the default max data size.
     */
    public SerializingTranscoder() {
        this(MAX_CONTENT_SIZE_IN_BYTES);
    }

    /**
     * Get a serializing transcoder that specifies the max data size.
     */
    public SerializingTranscoder(int max) {
        this(max, DEFAULT_COMPRESSION_THRESHOLD);
    }

    public SerializingTranscoder(int maxContentLength, int compressionThresholdInBytes) {
        this(maxContentLength, compressionThresholdInBytes, DEFAULT_COMPRESSOR);
    }

    public SerializingTranscoder(int maxContentLength, int compressionThresholdInBytes, Compression compressor) {
        this(maxContentLength, compressionThresholdInBytes, compressor, DEFAULT_METRIC_RECORDER);
    }

    public SerializingTranscoder(int maxContentLength, int compressionThresholdInBytes,
                                 Compression compressor, MetricRecorder metricRecorder) {
        super(maxContentLength, compressionThresholdInBytes, compressor);
        this.metricRecorder = metricRecorder;
    }

    @Override
    public boolean asyncDecode(CachedData d) {
        if ((d.getFlags() & COMPRESSED) != 0 || (d.getFlags() & SERIALIZED) != 0) {
            return true;
        }
        return super.asyncDecode(d);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.spy.memcached.Transcoder#decode(net.spy.memcached.CachedData)
     */
    public Object decode(CachedData d) {
        byte[] data = d.getData();

        Object rv = null;
        if ((d.getFlags() & COMPRESSED) != 0) {
            data = decompress(d.getData());
        }
        int flags = d.getFlags() & SPECIAL_MASK;
        if ((d.getFlags() & SERIALIZED) != 0 && data != null) {
            rv = deserialize(data);
        } else if (flags != 0 && data != null) {
            switch (flags) {
                case SPECIAL_BOOLEAN:
                    rv = Boolean.valueOf(tu.decodeBoolean(data));
                    break;
                case SPECIAL_INT:
                    rv = Integer.valueOf(tu.decodeInt(data));
                    break;
                case SPECIAL_LONG:
                    rv = Long.valueOf(tu.decodeLong(data));
                    break;
                case SPECIAL_DATE:
                    rv = new Date(tu.decodeLong(data));
                    break;
                case SPECIAL_BYTE:
                    rv = Byte.valueOf(tu.decodeByte(data));
                    break;
                case SPECIAL_FLOAT:
                    rv = new Float(Float.intBitsToFloat(tu.decodeInt(data)));
                    break;
                case SPECIAL_DOUBLE:
                    rv = new Double(Double.longBitsToDouble(tu.decodeLong(data)));
                    break;
                case SPECIAL_BYTEARRAY:
                    rv = data;
                    break;
                default:
                    logger.warn("Undecodeable with flags {}", flags);
            }
        } else {
            rv = decodeString(data);
        }
        metricRecorder.updateHistogram(DECODED_BYTES_METRIC_NAME,data.length);
        return rv;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.spy.memcached.Transcoder#encode(java.lang.Object)
     */
    public CachedData encode(Object o) {
        byte[] b = null;
        int flags = 0;
        if (o instanceof String) {
            b = encodeString((String) o);
        } else if (o instanceof Long) {
            b = tu.encodeLong((Long) o);
            flags |= SPECIAL_LONG;
        } else if (o instanceof Integer) {
            b = tu.encodeInt((Integer) o);
            flags |= SPECIAL_INT;
        } else if (o instanceof Boolean) {
            b = tu.encodeBoolean((Boolean) o);
            flags |= SPECIAL_BOOLEAN;
        } else if (o instanceof Date) {
            b = tu.encodeLong(((Date) o).getTime());
            flags |= SPECIAL_DATE;
        } else if (o instanceof Byte) {
            b = tu.encodeByte((Byte) o);
            flags |= SPECIAL_BYTE;
        } else if (o instanceof Float) {
            b = tu.encodeInt(Float.floatToRawIntBits((Float) o));
            flags |= SPECIAL_FLOAT;
        } else if (o instanceof Double) {
            b = tu.encodeLong(Double.doubleToRawLongBits((Double) o));
            flags |= SPECIAL_DOUBLE;
        } else if (o instanceof byte[]) {
            b = (byte[]) o;
            flags |= SPECIAL_BYTEARRAY;
        } else {
            b = serialize(o);
            flags |= SERIALIZED;
        }
        assert b != null;
        if (b.length > getCompressionThreshold()) {
            byte[] compressed = compress(b);
            if (compressed.length < b.length) {
                logger.debug("Compressed {} from {} to {}",
                        o.getClass().getName(), b.length, compressed.length);
                b = compressed;
                flags |= COMPRESSED;
            } else {
                logger.info("Compression increased the size of {} from {} to {}",
                        o.getClass().getName(), b.length, compressed.length);
            }
        }
        metricRecorder.updateHistogram(ENCODED_BYTES_METRIC_NAME,b.length);
        return new CachedData(flags, b, getMaxSize());
    }
}

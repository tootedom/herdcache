/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
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
import net.spy.memcached.compat.CloseUtil;
import net.spy.memcached.compat.SpyObject;

import org.greencheek.caching.herdcache.memcached.spy.extensions.ClassLoaderObjectInputStream;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.Compression;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.SnappyCompression;
import org.greencheek.caching.herdcache.memcached.util.ResizableByteBufferNoBoundsCheckingBackedOutputStream;
import org.greencheek.caching.herdcache.memcached.util.ThreadUnsafeByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Base class for any transcoders that may want to work with serialized or
 * compressed data.
 */
public abstract class BaseSerializingTranscoder extends SpyObject {

    private static Logger logger = LoggerFactory.getLogger(BaseSerializingTranscoder.class);

    /**
     * Default compression threshold value.
     */
    public static final int DEFAULT_COMPRESSION_THRESHOLD = 4096;

    public static final Compression DEFAULT_COMPRESSOR = new SnappyCompression();

    private static final String DEFAULT_CHARSET = "UTF-8";

    private final int compressionThreshold;
    private final String charset = DEFAULT_CHARSET;
    private final int maxSize;


    private final Compression compressor;
    /**
     * Initialize a serializing transcoder with the given maximum data size.
     */
    public BaseSerializingTranscoder(int max) {
        this(max,DEFAULT_COMPRESSION_THRESHOLD);
    }

    public BaseSerializingTranscoder(int maxContentLength, int compressionThreshold) {
        this(maxContentLength,compressionThreshold,DEFAULT_COMPRESSOR);
    }

    public BaseSerializingTranscoder(int maxContentLength, int compressionThreshold, Compression compression) {
        super();
        this.maxSize = maxContentLength;
        this.compressionThreshold = compressionThreshold;
        this.compressor = compression;
    }

    public boolean asyncDecode(CachedData d) {
        return false;
    }

    /**
     * Get the bytes representing the given serialized object.
     */
    protected byte[] serialize(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv = null;
        ResizableByteBufferNoBoundsCheckingBackedOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ResizableByteBufferNoBoundsCheckingBackedOutputStream(4096);
            os = new ObjectOutputStream(bos);
            os.writeObject(o);
            os.close();
            bos.close();
            rv = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            CloseUtil.close(os);
            CloseUtil.close(bos);
        }
        return rv;
    }

    /**
     * Get the object represented by the given serialized bytes.
     */
    protected Object deserialize(byte[] in) {
        Object rv = null;
        ThreadUnsafeByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if (in != null) {
                bis = new ThreadUnsafeByteArrayInputStream(in);
                is = new ClassLoaderObjectInputStream(bis);
                rv = is.readObject();
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            logger.warn("Caught IOException decoding {} bytes of data",
                    in == null ? 0 : in.length, e);
        } catch (ClassNotFoundException e) {
            logger.warn("Caught CNFE decoding {} bytes of data",
                    in == null ? 0 : in.length, e);
        } finally {
            CloseUtil.close(is);
            CloseUtil.close(bis);
        }
        return rv;
    }

    /**
     * Compress the given array of bytes.
     */
    protected byte[] compress(byte[] in) {
        return compressor.compress(in);
    }

    /**
     * Decompress the given array of bytes.
     *
     * @return null if the bytes cannot be decompressed
     */
    protected byte[] decompress(byte[] in) {
        return compressor.decompress(in);
    }

    /**
     * Decode the string with the current character set.
     */
    protected String decodeString(byte[] data) {
        String rv = null;
        try {
            if (data != null) {
                rv = new String(data, charset);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return rv;
    }

    /**
     * Encode a string into the current character set.
     */
    protected byte[] encodeString(String in) {
        byte[] rv = null;
        try {
            rv = in.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return rv;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }
}

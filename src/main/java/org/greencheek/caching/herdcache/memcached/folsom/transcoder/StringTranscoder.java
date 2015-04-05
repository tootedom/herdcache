package org.greencheek.caching.herdcache.memcached.folsom.transcoder;

import com.google.common.base.Charsets;
import com.spotify.folsom.Transcoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.charset.Charset;


public class StringTranscoder implements Transcoder<String> {

    private static final Logger logger = LoggerFactory.getLogger(StringTranscoder.class);
    public static final StringTranscoder UTF8_INSTANCE = new StringTranscoder(Charsets.UTF_8);

    private final Charset charset;
    private final boolean compress;


    public StringTranscoder(final Charset charset) {
        this(charset,true);
    }

    public StringTranscoder(final Charset charset, boolean compress) {
        this.charset = charset;
        this.compress = compress;
    }

    @Override
    public byte[] encode(final String t) {
        byte[] bytes = t.getBytes(charset);;
        if(compress) {
            try {
                bytes = Snappy.compress(bytes);
            } catch (IOException e) {
                logger.warn("Unable to compress bytes",e);
            }
        }
        return bytes;
    }

    @Override
    public String decode(final byte[] b) {
        byte[] bytes = b;
        if(compress) {
            try {
                bytes = Snappy.uncompress(b);
            } catch (IOException e) {
                logger.warn("Unable to decompress bytes",e);
            }
        }

        return new String(bytes, charset);
    }

}
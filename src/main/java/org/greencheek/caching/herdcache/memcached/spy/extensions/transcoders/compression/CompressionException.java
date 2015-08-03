package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression;

/**
 *
 */
public class CompressionException extends RuntimeException {


    public CompressionException(String message) {
        super(message);
    }

    public CompressionException(String message, Throwable e) {
        super(message,e);
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}

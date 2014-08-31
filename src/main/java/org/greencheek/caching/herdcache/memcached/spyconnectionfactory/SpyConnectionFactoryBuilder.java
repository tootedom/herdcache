package org.greencheek.caching.herdcache.memcached.spyconnectionfactory;

import net.spy.memcached.*;
import net.spy.memcached.transcoders.Transcoder;
import org.greencheek.caching.herdcache.memcached.keyhashing.*;
import org.greencheek.caching.herdcache.memcached.spy.extensions.connection.CustomConnectionFactoryBuilder;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class SpyConnectionFactoryBuilder {

    public static ConnectionFactory createConnectionFactory(
            ConnectionFactoryBuilder.Locator hashingType,
            FailureMode failureMode,
            HashAlgorithm hashAlgorithm,
            Transcoder<Object> serializingTranscoder,
            ConnectionFactoryBuilder.Protocol protocol,
            int readBufferSize,
            KeyHashingType keyHashType) {

        ConnectionFactoryBuilder builder =  (keyValidationRequired(keyHashType)==true) ?
                new ConnectionFactoryBuilder() :  new CustomConnectionFactoryBuilder();

        builder.setHashAlg(hashAlgorithm);
        builder.setLocatorType(hashingType);
        builder.setProtocol(protocol);
        builder.setReadBufferSize(readBufferSize);
        builder.setFailureMode(failureMode);
        builder.setTranscoder(serializingTranscoder);

        return builder.build();
    }

    private static boolean keyValidationRequired(KeyHashingType type ) {
        switch (type) {
            case NONE:
                return true;
            case NATIVE_XXHASH:
                return false;
            case JAVA_XXHASH:
                return false;
            case MD5_UPPER:
                return false;
            case SHA256_UPPER:
                return false;
            case MD5_LOWER:
                return false;
            case SHA256_LOWER:
                return false;
            default:
                return false;
        }
    }
}

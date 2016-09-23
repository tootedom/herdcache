package org.greencheek.caching.herdcache.memcached.spyconnectionfactory;

import net.spy.memcached.*;
import net.spy.memcached.transcoders.Transcoder;
import org.greencheek.caching.herdcache.memcached.keyhashing.*;
import org.greencheek.caching.herdcache.memcached.spy.extensions.connection.CustomConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.locator.LocatorFactory;

import java.util.concurrent.ExecutorService;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class SpyConnectionFactoryBuilder {

    public static ConnectionFactory createConnectionFactory(
            FailureMode failureMode,
            HashAlgorithm hashAlgorithm,
            Transcoder<Object> serializingTranscoder,
            ConnectionFactoryBuilder.Protocol protocol,
            int readBufferSize,
            KeyHashingType keyHashType,
            LocatorFactory locatorFactory) {

        return createConnectionFactory(failureMode,hashAlgorithm,serializingTranscoder,
                                       protocol,readBufferSize,keyHashType,locatorFactory,null);
    }

    public static ConnectionFactory createConnectionFactory(
            FailureMode failureMode,
            HashAlgorithm hashAlgorithm,
            Transcoder<Object> serializingTranscoder,
            ConnectionFactoryBuilder.Protocol protocol,
            int readBufferSize,
            KeyHashingType keyHashType,
            LocatorFactory locatorFactory,
            ExecutorService executorService) {

        CustomConnectionFactoryBuilder builder = new CustomConnectionFactoryBuilder();
        builder.setHashAlg(hashAlgorithm);
        builder.setProtocol(protocol);
        builder.setReadBufferSize(readBufferSize);
        builder.setFailureMode(failureMode);
        builder.setTranscoder(serializingTranscoder);
        builder.setLocatorFactory(locatorFactory);
        if (executorService !=null) {
            builder.setListenerExecutorService(executorService);
        }

        return builder.build(keyValidationRequired(keyHashType));
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

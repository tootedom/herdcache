package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders;

import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import net.spy.memcached.CachedData;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.Compression;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.LZ4NativeCompression;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.SnappyCompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Uses https://github.com/RuedigerMoeller/fast-serialization for serialization
 */
public class FastSerializingTranscoder extends SerializingTranscoder {
    private static final Logger logger = LoggerFactory.getLogger(FastSerializingTranscoder.class);
    public static final boolean DEFAULT_SHARE_REFERENCES = true;
    public static final int MAX_CONTENT_SIZE_IN_BYTES = CachedData.MAX_SIZE;

    // ! reuse this Object, it caches metadata. Performance degrades massively
    // if you create a new Configuration Object with each serialization !
    final FSTConfiguration conf;

    public FastSerializingTranscoder() {
        this(new FastSerializingTranscoderConfigBuilder().build());
    }

    public FastSerializingTranscoder(Compression compression) {
        this(new FastSerializingTranscoderConfigBuilder().setCompression(compression).build());
    }


    public FastSerializingTranscoder(int maxContentLengthInBytes, int compressionThresholdInBytes) {
        this(
                new FastSerializingTranscoderConfigBuilder()
                    .setMaxContentLengthInBytes(maxContentLengthInBytes)
                    .setCompressionThresholdInBytes(compressionThresholdInBytes)
                    .build()
        );
    }

    public FastSerializingTranscoder(Class[] classesKnownToBeSerialized) {
        this(new FastSerializingTranscoderConfigBuilder().setClassesKnownToBeSerialized(classesKnownToBeSerialized).build());
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized) {
        this(
                new FastSerializingTranscoderConfigBuilder()
                .setShareReferences(shareReferences)
                .setClassesKnownToBeSerialized(classesKnownToBeSerialized)
                .build()
        );
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized,Compression compression) {
        this(
                new FastSerializingTranscoderConfigBuilder()
                .setShareReferences(shareReferences)
                .setClassesKnownToBeSerialized(classesKnownToBeSerialized)
                .setCompression(compression)
                .build()
        );

    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized,
                                     int maxContentLengthInBytes, int compressionThresholdInBytes) {
        this(
                new FastSerializingTranscoderConfigBuilder()
                .setShareReferences(shareReferences)
                .setClassesKnownToBeSerialized(classesKnownToBeSerialized)
                .setMaxContentLengthInBytes(maxContentLengthInBytes)
                .setCompressionThresholdInBytes(compressionThresholdInBytes)
                .build()
        );
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized,
                                     int maxContentLengthInBytes, int compressionThresholdInBytes,
                                     Compression compression
    ) {
        this(
                new FastSerializingTranscoderConfigBuilder()
                .setShareReferences(shareReferences)
                .setClassesKnownToBeSerialized(classesKnownToBeSerialized)
                .setMaxContentLengthInBytes(maxContentLengthInBytes)
                .setCompressionThresholdInBytes(compressionThresholdInBytes)
                .setCompression(compression)
                .build()
        );
    }

    public FastSerializingTranscoder(FastSerializingTranscoderConfig config) {
        super(config.getMaxContentLengthInBytes(),
                config.getCompressionThresholdInBytes(),
                config.getCompression(),
                config.getMetricRecorder());
        conf = FSTConfiguration.createDefaultConfiguration();
        conf.setShareReferences(config.isShareReferences());
        Class[] classesKnownToBeSerialized = config.getClassesKnownToBeSerialized();
        if (classesKnownToBeSerialized != null && classesKnownToBeSerialized.length > 0) {
            conf.registerClass(classesKnownToBeSerialized);
        }
    }

    /**
     * Get the object represented by the given serialized bytes.
     */
    protected Object deserialize(byte[] in) {
        Object rv = null;

        try {
            if (in != null) {
                FSTObjectInput is = conf.getObjectInput(in);
                rv = is.readObject();
            }
        } catch (IOException e) {
            logger.warn("Caught IOException decoding {} bytes of data",
                    in == null ? 0 : in.length, e);
        } catch (ClassNotFoundException e) {
            logger.warn("Caught CNFE decoding {} bytes of data",
                    in == null ? 0 : in.length, e);
        }

        return rv;
    }

    /**
     * Get the bytes representing the given serialized object.
     */
    protected byte[] serialize(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv = null;
        FSTObjectOutput os=null;
        try {
            os = conf.getObjectOutput();
            os.writeObject(o);
            os.flush();
            rv = os.getCopyOfWrittenBuffer();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            if(os!=null) {
                os.resetForReUse(new byte[0]);
            }
        }
        return rv;
    }


}

package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders;

import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import net.spy.memcached.CachedData;
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
        this(DEFAULT_SHARE_REFERENCES,null);
    }

    public FastSerializingTranscoder(Compression compression) {
        this(DEFAULT_SHARE_REFERENCES,null,compression);
    }


    public FastSerializingTranscoder(int maxContentLengthInBytes, int compressionThresholdInBytes) {
        this(DEFAULT_SHARE_REFERENCES,null,maxContentLengthInBytes,compressionThresholdInBytes);
    }

    public FastSerializingTranscoder(Class[] classesKnownToBeSerialized) {
        this(DEFAULT_SHARE_REFERENCES,classesKnownToBeSerialized);
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized) {
        this(shareReferences,classesKnownToBeSerialized,MAX_CONTENT_SIZE_IN_BYTES,DEFAULT_COMPRESSION_THRESHOLD);
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized,Compression compression) {
        this(shareReferences,classesKnownToBeSerialized,MAX_CONTENT_SIZE_IN_BYTES,DEFAULT_COMPRESSION_THRESHOLD,compression);
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized,
                                     int maxContentLengthInBytes, int compressionThresholdInBytes) {
        this(shareReferences,classesKnownToBeSerialized,maxContentLengthInBytes,compressionThresholdInBytes,new LZ4NativeCompression());
    }

    public FastSerializingTranscoder(boolean shareReferences, Class[] classesKnownToBeSerialized,
                                     int maxContentLengthInBytes, int compressionThresholdInBytes,
                                     Compression compression
    ) {
        super(maxContentLengthInBytes,compressionThresholdInBytes,compression);
        conf = FSTConfiguration.createDefaultConfiguration();
        conf.setShareReferences(shareReferences);
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

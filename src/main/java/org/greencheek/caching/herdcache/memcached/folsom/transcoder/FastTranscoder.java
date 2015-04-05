package org.greencheek.caching.herdcache.memcached.folsom.transcoder;

import com.spotify.folsom.Transcoder;
import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * Created by dominictootell on 05/04/2015.
 */
public class FastTranscoder implements Transcoder {

    private static final Logger logger = LoggerFactory.getLogger(FastTranscoder.class);

    private final boolean compress;


    // ! reuse this Object, it caches metadata. Performance degrades massively
    // if you create a new Configuration Object with each serialization !
    private final FSTConfiguration conf;

    public FastTranscoder() {
        this(true);
    }

    public FastTranscoder(boolean compress) {
        conf = FSTConfiguration.createDefaultConfiguration();
        conf.setShareReferences(true);
        this.compress = compress;
    }

    @Override
    public Object decode(byte[] in) {
        Object rv = null;

        try {
            if (in != null) {
                FSTObjectInput is = null;
                if(compress) {
                    is = conf.getObjectInput(Snappy.uncompress(in));
                } else {
                    is = conf.getObjectInput(in);
                }
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

    @Override
    public byte[] encode(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv = null;
        try {
            FSTObjectOutput os = conf.getObjectOutput();
            os.writeObject(o);
            os.flush();
            rv = os.getCopyOfWrittenBuffer();

            if(compress) {
                rv = Snappy.compress(rv);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        }
        return rv;
    }
}

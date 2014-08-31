package org.greencheek.caching.herdcache.memcached.keyhashing;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by dominictootell on 09/04/2014.
 */
public class MessageDigestHashing implements KeyHashing {

    private final boolean upperCase;

    private final ToHexString byteToHexStringConverter;
    private final ArrayBlockingQueue<MessageDigest> digesters;

    public MessageDigestHashing() {
        this(KeyHashing.MD5);
    }

    public MessageDigestHashing(String algorithm) {
        this(algorithm, Runtime.getRuntime().availableProcessors()*2);
    }

    public MessageDigestHashing(String algorithm,int messageDigests) {
        this(algorithm,messageDigests,true);
    }

    public MessageDigestHashing(String algorithm,int messageDigests,boolean toUpper) {
        this.upperCase = toUpper;

        if(upperCase) {
            byteToHexStringConverter = UpperCaseToHexString.INSTANCE;
        } else {
            byteToHexStringConverter = LowerCaseToHexString.INSTANCE;
        }

        try {
            digesters = createDigests(messageDigests, algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new InstantiationError("Unable to create MessageDigest for algo: " + algorithm);
        }
    }


    private final ArrayBlockingQueue<MessageDigest> createDigests(int numToCreate, String algorithm)
    throws NoSuchAlgorithmException {
        ArrayBlockingQueue<MessageDigest> queue = new ArrayBlockingQueue<MessageDigest>(numToCreate);
        for(int i = 0;i<numToCreate;i++) {
            queue.add(MessageDigest.getInstance(algorithm));
        }
        return queue;
    }

    @Override
    public String hash(String key) {
        byte[] bytes;
        try {
            bytes = key.getBytes("UTF-8");
        } catch(UnsupportedEncodingException e) {
            bytes = key.getBytes();
        }
        return hash(bytes,0,bytes.length);
    }

    @Override
    public String hash(byte[] bytes,int offset, int length) {
        MessageDigest md;
        try {
            md = digesters.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        md.update(bytes, offset, length);
        byte[] result = md.digest();
        md.reset();
        digesters.add(md);
        return byteToHexStringConverter.bytesToHex(result);
    }
}

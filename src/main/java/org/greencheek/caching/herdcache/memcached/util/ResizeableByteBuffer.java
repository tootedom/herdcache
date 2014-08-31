package org.greencheek.caching.herdcache.memcached.util;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ResizeableByteBuffer {
    // without oops array header is 8 bytes so max array size is that minus
    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private byte[] buf;
    private volatile int position;
    private volatile boolean canWrite = true;

    private final int maxCapacity;


    public ResizeableByteBuffer(int maxCapacity) {
        this(4096,maxCapacity);
    }

    public ResizeableByteBuffer(int initialCapacity, int maxCapacity) {
        if(maxCapacity > MAX_ARRAY_SIZE) {
            maxCapacity = MAX_ARRAY_SIZE;
        }

        if(maxCapacity<initialCapacity) {
            initialCapacity = maxCapacity;
        }

        buf = new byte[initialCapacity];
        this.maxCapacity = maxCapacity;
    }

    public int size() {
        return position;
    }

    public void setSize(int i) {
        if(i>-1) {
            this.position = i;
        }
    }

    public void reset() {
        canWrite = true;
        position = 0;
    }

    public void closeForWrites() {
        canWrite = false;
    }


    public boolean canWrite() {
        return canWrite;
    }

    public byte[] getBuf() {
        return buf;
    }

    public int position() {
        return position;
    }

    public byte[] toByteArray() {
        final byte[] bytes = new byte[position];
        System.arraycopy(buf, 0, bytes, 0, position);
        return bytes;
    }

    public ResizeableByteBuffer trim() {
        final byte[] bytes = new byte[position];
        System.arraycopy(buf, 0, bytes, 0, position);
        buf = bytes;
        return this;
    }


    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buf, 0, position);
    }


    public void append(byte b) {
        if(canWrite && checkSizeAndGrow(1)) {
            appendNoResize(b);
        }
    }

    public void append(byte[] bytes) {
        if(canWrite) {
            int len = bytes.length;
            if(checkSizeAndGrow(len)) {
                appendNoResize(bytes, len);
            }
        }
    }

    public void append(byte[] b, int off, int len) {
        if(canWrite && checkSizeAndGrow(len)) {
            System.arraycopy(b, off, buf, position, len);
            position += len;
        }
    }

//    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        append(b,off,len);
    }


//    @Override
    public void write(int b) throws IOException {
        append((byte)b);
    }

//    @Override
    public void write(byte[] b) {
        append(b);
    }


    private void appendNoResize(byte c) {
        buf[position++]=c;
    }

    private void appendNoResize(byte[] bytes,int len) {
        System.arraycopy(bytes, 0, buf, position, len);
        position+=len;
    }

    private boolean hasSpace(int extra) {
        return (extra+position <= buf.length);
    }

    private boolean checkSizeAndGrow(int extra) {
        if(hasSpace(extra)) return true;

        grow(extra);
        return canWrite;

    }

    private void grow(int extra) {
        int currentCapacity = buf.length;
        int requiredCapacity = position+extra;

        int newSize = currentCapacity*2;
        if(newSize>=maxCapacity) {
            newSize = maxCapacity;
            // new size is less than the required capacity
            if(newSize<requiredCapacity) {
                canWrite = false;
            }
        } else {
            if(newSize<requiredCapacity) {
                newSize = requiredCapacity;
            }

            if(requiredCapacity>maxCapacity) {
                canWrite = false;
            }
        }

        if(canWrite) {
            byte[] newBuf = new byte[newSize];
            System.arraycopy(buf, 0, newBuf, 0, position);
            buf = newBuf;
        }
    }
}
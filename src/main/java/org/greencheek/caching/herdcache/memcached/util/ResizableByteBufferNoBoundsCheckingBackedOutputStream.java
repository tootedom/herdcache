
package org.greencheek.caching.herdcache.memcached.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class ResizableByteBufferNoBoundsCheckingBackedOutputStream extends OutputStream {

    private final ResizeableByteBuffer byteBuffer;

    public ResizableByteBufferNoBoundsCheckingBackedOutputStream(int initialCapacity) {
        this(initialCapacity,ResizeableByteBuffer.MAX_ARRAY_SIZE);
    }

    public ResizableByteBufferNoBoundsCheckingBackedOutputStream(int initialCapacity, int maxCapacity) {
        this.byteBuffer = new ResizeableByteBuffer(initialCapacity,maxCapacity);
    }


    public ResizeableByteBuffer getBuffer() {
        return byteBuffer;
    }

    public int size() {
        return byteBuffer.size();
    }


    public void reset() {
        byteBuffer.reset();
    }


    public byte[] getBuf() {
        return byteBuffer.getBuf();
    }

    public byte[] toByteArray() {
        return byteBuffer.toByteArray();
    }


    public ByteBuffer toByteBuffer() {
        return byteBuffer.toByteBuffer();
    }

    public void append(byte b) {
        byteBuffer.append(b);
        if(!byteBuffer.canWrite()) {
            throw new BufferOverflowException();
        }
    }

    public void append(byte[] bytes) {
        byteBuffer.append(bytes);
        if(!byteBuffer.canWrite()) {
            throw new BufferOverflowException();
        }
    }

    public void append(byte[] b, int off, int len) {
        byteBuffer.append(b,off,len);
        if(!byteBuffer.canWrite()) {
            throw new BufferOverflowException();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            append(b, off, len);
        } catch(BufferOverflowException e) {
            throw new IOException(e.getMessage(),e);
        }
    }


    @Override
    public void write(int b) throws IOException {
        try {
            append((byte) b);
        } catch (BufferOverflowException e) {
            throw new IOException(e.getMessage(),e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        if(b==null) throw new NullPointerException();
        write(b,0,b.length);
    }
}
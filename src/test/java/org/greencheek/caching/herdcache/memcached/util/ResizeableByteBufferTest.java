package org.greencheek.caching.herdcache.memcached.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.*;

/**
 * Created by dominictootell on 29/03/2014.
 */
public class ResizeableByteBufferTest
{
    public ResizeableByteBuffer getResizeableByteBuffer(int max) {

        return new ResizeableByteBuffer(max);
    }

    public ResizeableByteBuffer getResizeableByteBuffer(int min,int max) {

        return new ResizeableByteBuffer(min,max);
    }


    @Test
    public void testCanWriteAByte() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(1);

        buffer.write(88);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(1, b.limit());

        assertEquals(88,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }

        byte[] byteArray = buffer.toByteArray();

        assertEquals(1,byteArray.length);
        assertEquals(88,byteArray[0]);
    }


    @Test
    public void testCanWriteBytes() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(256);

        buffer.write((byte) 88);
        buffer.write(new byte[]{1, 2, 3, 4});
        buffer.write((byte) 6);
        byte[] byteArray = buffer.toByteArray();

        assertEquals(6,byteArray.length);
        assertEquals(88,byteArray[0]);
        assertArrayEquals(new byte[]{88,1,2,3,4,6},byteArray);
    }

    @Test
    public void testCanWriteByteSubSet() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(256);

        buffer.write((byte) 88);
        buffer.write(new byte[]{1, 2, 3, 4});
        buffer.write(new byte[]{1, 2, 3, 4},1,2);
        buffer.write((byte) 6);
        byte[] byteArray = buffer.toByteArray();

        assertEquals(8,byteArray.length);
        assertEquals(88,byteArray[0]);
        assertArrayEquals(new byte[]{88,1,2,3,4,2,3,6},byteArray);
    }


    @Test
    public void testCanAppend() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(2);
        buffer.append((byte)88);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(1, b.limit());

        assertEquals(88,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }

    @Test
    public void xtestCanAppendSizeAndMultipleBytes() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(16);
        buffer.append((byte)88);
        buffer.append(new byte[]{1,2,3,4});
        buffer.append((byte)6);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(6, b.limit());

        assertEquals(88,b.get());
        assertEquals(1,b.get());
        assertEquals(2,b.get());
        assertEquals(3,b.get());
        assertEquals(4,b.get());
        assertEquals(6,b.get());
    }

    @Test
    public void testCanGetUnderlyingByteArray() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(4);
        buffer.append((byte)88);
        buffer.append((byte)88);
        buffer.append((byte)88);

        assertNotNull(buffer.getBuf());

        assertTrue(buffer.getBuf().length>2);
    }

    @Test
    public void testCanGrow() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(1,2);
        buffer.append((byte)88);
        buffer.append((byte)89);


        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(2,b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }

    @Test
    public void testSizeIsCorrect() {
        ResizeableByteBuffer buffer =  getResizeableByteBuffer(2);
        buffer.append((byte)88);
        buffer.append((byte)89);

        assertEquals(2, buffer.size());

        buffer =  getResizeableByteBuffer(4);
        buffer.append((byte)88);
        buffer.append((byte)89);
        buffer.append((byte)89);

        assertEquals(3,buffer.size());
    }

    @Test
    public void testCannotGrowOverMaxSize() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(2);
        buffer.append((byte)88);
        buffer.append((byte)89);
        try {
            buffer.append((byte)90);
            assertFalse("should not be able to append more than the maximum",buffer.canWrite());
            assertEquals("should not be able to append more than the maximum",2,buffer.size());
        } catch(BufferOverflowException e) {

        }


        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(2,b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }


    @Test
    public void testCannotGrowOverMaxSizeWithSameMinMaxInitialisation() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(2);
        buffer.append((byte)88);
        buffer.append((byte)89);
        try {
            buffer.append((byte)90);
            assertFalse("should not be able to append more than the maximum",buffer.canWrite());
            assertEquals("should not be able to append more than the maximum",2,buffer.size());
        } catch(BufferOverflowException e) {

        }


        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(2,b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }

    @Test
    public void testCannotGrowOverMaxSizeWithByteArrayAppend() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(2);
        buffer.append((byte)88);


            buffer.append(new byte[]{89,90});
            assertFalse("should not be able to append more than the maximum",buffer.canWrite());
            assertEquals("should not be able to append more than the maximum",1,buffer.size());


        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(1,b.limit());
        assertEquals(88,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }


    @Test
    public void testCanResetBuffer() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(10);
        buffer.append((byte)90);
        buffer.append(new byte[]{91,92,93,94,95,96,97,98,99});



        try {

            buffer.append((byte) 100);
            assertFalse("should not be able to add another byte",buffer.canWrite());
            assertEquals(10, buffer.size());
        } catch(BufferOverflowException e) {

        }

        buffer.reset();
        buffer.append(new byte[]{1,2,3,4,5,6,7,8,9,10});

        try {
            buffer.append((byte) 11);
            assertFalse("should not be able to add another byte", buffer.canWrite());
        } catch(BufferOverflowException e) {

        }

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(10,b.limit());


        assertEquals(1,b.get());
        assertEquals(2,b.get());
        assertEquals(3,b.get());
        assertEquals(4,b.get());
        assertEquals(5,b.get());
        assertEquals(6,b.get());
        assertEquals(7,b.get());
        assertEquals(8,b.get());
        assertEquals(9,b.get());
        assertEquals(10,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }

    @Test
    public void testCanResetBufferWithPartialByteArrayAppend() {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(10);
        buffer.append((byte)90);
        buffer.append(new byte[]{91,92,93,94,95,96,97,98,99});




            buffer.append((byte)100);
            assertFalse("should not be able to append more than the maximum",buffer.canWrite());
            assertEquals("should not be able to append more than the maximum",10,buffer.size());


        buffer.reset();
        buffer.append(new byte[]{1,2,3,4,5,6,7,8,9,10},0,5);
        buffer.append(new byte[]{1,2,3,4,5,6,7,8,9,10},6,4);

        try {
            buffer.append((byte)6);
            assertTrue(buffer.canWrite());
        } catch(BufferOverflowException e) {
            fail("should be able to add another byte");
        }

        try {
            buffer.append((byte)11);
            assertFalse("should not be able to add another byte",buffer.canWrite());

        } catch(BufferOverflowException e) {
        }

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(10,b.limit());


        assertEquals(1,b.get());
        assertEquals(2,b.get());
        assertEquals(3,b.get());
        assertEquals(4,b.get());
        assertEquals(5,b.get());
        assertEquals(7,b.get());
        assertEquals(8,b.get());
        assertEquals(9,b.get());
        assertEquals(10,b.get());
        assertEquals(6,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }
    }

    @Test
    public void testCapacityExpansion() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(1,4);

        buffer.write(new byte[]{88,89,99},0,3);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(3, b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());
        assertEquals(99,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }

        byte[] byteArray = buffer.toByteArray();

        assertEquals(3,byteArray.length);
        assertEquals(88,byteArray[0]);
    }

    @Test
    public void testPosition() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(1,3);

        buffer.write(new byte[]{88,89,99},0,3);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(3, b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());
        assertEquals(99,b.get());

        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }

        assertEquals(3,buffer.position());

        buffer.write(new byte[]{88,89,99},0,3);
        buffer.write(new byte[]{88,89,99},0,3);
        assertEquals(3,buffer.position());



    }


    @Test
    public void testAppendAfterFull() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(1,2);

        buffer.write(new byte[]{88,89},0,2);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(2, b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());


        try {
            b.get();
            fail("should not be able to get another byte");
        } catch(BufferUnderflowException e) {

        }

        byte[] byteArray = buffer.toByteArray();

        assertEquals(2,byteArray.length);
        assertEquals(88,byteArray[0]);

        buffer.write(new byte[]{88,89},0,2);
        assertFalse(buffer.canWrite());
        buffer.write(new byte[]{88,89},0,2);

    }

    @Test
    public void testCloseForWrites() throws IOException {
        ResizeableByteBuffer buffer = getResizeableByteBuffer(1,10);

        buffer.write(new byte[]{88,89},0,2);

        ByteBuffer b =  buffer.toByteBuffer();
        assertEquals(2, b.limit());

        assertEquals(88,b.get());
        assertEquals(89,b.get());

        buffer.append((byte)100);
        buffer.closeForWrites();

        buffer.append((byte)1);
        buffer.append((byte)2);


        byte[] byteArray = buffer.toByteArray();

        assertEquals(3,byteArray.length);
        assertEquals(88,byteArray[0]);
        assertEquals(100,byteArray[2]);


    }
}

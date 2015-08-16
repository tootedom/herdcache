package org.greencheek.caching.herdcache.memcached.spy.extensions.locator;

import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;


public class ArrayBasedCeilRingTest {

    @Test
    public void testArrayContainingAllNegatives() throws Exception {
        long arr[] = new long[] { -5, -3, -1};
        int lastIndex = arr.length-1;

        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(9, arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(11,arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(Integer.MAX_VALUE, arr,lastIndex));   // 0

        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(-10,arr,lastIndex));   // 0
        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(-4,arr,lastIndex));   // 1
        assertEquals(2,ArrayBasedCeilRing.findCeilIndex(-2,arr,lastIndex));   // 2
    }

    @Test
    public void testSingleValueArray() throws Exception {
        long arr[] = new long[] {10};
        int lastIndex = arr.length-1;

        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(9, arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(10, arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(11,arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(Integer.MAX_VALUE, arr,lastIndex));   // 0
    }

    @Test
    public void testDoubleValueArray() throws Exception {
        long arr[] = new long[] {5,10};
        int lastIndex = arr.length-1;

        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(4, arr, lastIndex));   // 0
        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(6,arr, lastIndex));   // 0
        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(6,arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(Integer.MAX_VALUE, arr,lastIndex));   // 0
    }

    @Test
    public void testThreeValueArray() throws Exception {
        long arr[] = new long[] {5,7,10};
        int lastIndex = arr.length-1;

        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(6, arr, lastIndex));   // 0
        assertEquals(2,ArrayBasedCeilRing.findCeilIndex(8,arr, lastIndex));   // 0
        assertEquals(2,ArrayBasedCeilRing.findCeilIndex(10,arr, lastIndex));   // 0
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(Integer.MAX_VALUE, arr,lastIndex));   // 0
    }

    @Test
    public void testNegativeToPositiveArray() {
        // goes from -10000 (in evens) to 9998
        long arr[] = new long[10000];
        int j = -10000;
        for(int i = 0;i<10000;i++) {
            arr[i] = j + (i * 2);

        }

        int lastIndex = arr.length-1;

        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(-10000, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(-11000, arr, lastIndex));
        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(-9999, arr, lastIndex));
        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(-9998, arr, lastIndex));
        assertEquals(4,ArrayBasedCeilRing.findCeilIndex(-9993, arr, lastIndex));
        assertEquals(9999,ArrayBasedCeilRing.findCeilIndex(9998, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(14999, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(14996, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(156674996, arr, lastIndex));
        assertEquals(6000,ArrayBasedCeilRing.findCeilIndex(2000, arr, lastIndex));
        assertEquals(5000,ArrayBasedCeilRing.findCeilIndex(0, arr, lastIndex));
        assertEquals(5001,ArrayBasedCeilRing.findCeilIndex(1, arr, lastIndex));
        assertEquals(4999,ArrayBasedCeilRing.findCeilIndex(-2, arr, lastIndex));
        assertEquals(5000,ArrayBasedCeilRing.findCeilIndex(-1, arr, lastIndex));
        assertEquals(5005,ArrayBasedCeilRing.findCeilIndex(9, arr, lastIndex));

    }

    @Test
    public void testRandomPositiveArray() {
        long arr[] = {1, 2, 8, 10, 12, 19, 21, 66, 67, 68, 69, 77, 101};
        int lastIndex = arr.length-1;

        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(-1, arr, lastIndex));
        assertEquals(6,ArrayBasedCeilRing.findCeilIndex(20, arr, lastIndex));
        assertEquals(5,ArrayBasedCeilRing.findCeilIndex(18, arr, lastIndex));
        assertEquals(3,ArrayBasedCeilRing.findCeilIndex(9, arr, lastIndex));
        assertEquals(2,ArrayBasedCeilRing.findCeilIndex(3, arr, lastIndex));
        assertEquals(2,ArrayBasedCeilRing.findCeilIndex(8, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(1, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(0, arr, lastIndex));
        assertEquals(5,ArrayBasedCeilRing.findCeilIndex(19, arr, lastIndex));
        assertEquals(4,ArrayBasedCeilRing.findCeilIndex(11, arr, lastIndex));
        assertEquals(1,ArrayBasedCeilRing.findCeilIndex(2, arr, lastIndex));
        assertEquals(5,ArrayBasedCeilRing.findCeilIndex(13, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(Integer.MAX_VALUE, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(Integer.MIN_VALUE, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(1, arr, lastIndex));
    }

    @Test
    public void test10000PositiveArray() {
        long[] arr = new long[10000];
        for(int i = 0;i<10000;i++) {
            arr[i] = i*2;
        }
        int lastIndex = arr.length-1;


        assertEquals(9999,ArrayBasedCeilRing.findCeilIndex(19998, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(19999, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(20000, arr, lastIndex));
        assertEquals(1000,ArrayBasedCeilRing.findCeilIndex(2000, arr, lastIndex));
        assertEquals(500,ArrayBasedCeilRing.findCeilIndex(999, arr, lastIndex));
        assertEquals(500,ArrayBasedCeilRing.findCeilIndex(1000, arr, lastIndex));
        assertEquals(0,ArrayBasedCeilRing.findCeilIndex(-99, arr, lastIndex));
    }

    @Test
    public void testExceptionThrownOnEmptyArray() {
        long[] arr = new long[0];
        int lastIndex = arr.length-1;

        try {
            ArrayBasedCeilRing.findCeilIndex(Integer.MIN_VALUE,arr,lastIndex);
            fail("IllegalArgumentException expect to be thrown on an empty array");
        } catch(IllegalArgumentException e) {
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testEmptyMapThrowsException() {
        Map<Long,MemcachedNode> nodes = new HashMap<Long,MemcachedNode>();
        ArrayBasedCeilRing ring = new ArrayBasedCeilRing(nodes, Collections.EMPTY_LIST);

        try {
            ring.findCeilIndex(0);
            fail("IllegalArgumentException expect to be thrown on an empty array");
        } catch(IllegalArgumentException e) {
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testMapResultsInCorrectlySortedRing() {
        Map<Long,MemcachedNode> nodes = new HashMap<Long,MemcachedNode>();
        List<MemcachedNode> nodesRepresented = new ArrayList(2);

        MemcachedNode node1 = new RingTestingMemcachedNode(1234);
        MemcachedNode node2 = new RingTestingMemcachedNode(-100234);

        nodesRepresented.add(node1);
        nodesRepresented.add(node2);
        // 1, 2, 8, 10, 10, 12, 19, 21, 66, 67 };
        nodes.put(8l,node1);
        nodes.put(1l,node1);
        nodes.put(19l,node1);
        nodes.put(10l,node1);
        nodes.put(2l,node2);
        nodes.put(21l,node2);
        nodes.put(67l,node2);
        nodes.put(12l,node2);
        nodes.put(66l,node2);


        ArrayBasedCeilRing ring = new ArrayBasedCeilRing(nodes,nodesRepresented);

        assertEquals(2,ring.getAllNodes().size());
        assertEquals(67,ring.getMaxPosition());

        assertEquals(1234,ring.findClosestNode(7).lastReadDelta());
        assertEquals(-100234,ring.findClosestNode(62).lastReadDelta());


    }

    @Test
    public void testAsMap() {
        Map<Long,MemcachedNode> nodes = new HashMap<Long,MemcachedNode>();
        List<MemcachedNode> nodesRepresented = new ArrayList(2);

        MemcachedNode node1 = new RingTestingMemcachedNode(1234);
        MemcachedNode node2 = new RingTestingMemcachedNode(-100234);

        nodesRepresented.add(node1);
        nodesRepresented.add(node2);
        // 1, 2, 8, 10, 10, 12, 19, 21, 66, 67 };
        nodes.put(1l,node1);
        nodes.put(2l,node1);
        nodes.put(3l,node1);
        nodes.put(4l,node1);
        nodes.put(5l,node2);

        ArrayBasedCeilRing ring = new ArrayBasedCeilRing(nodes,nodesRepresented);

        Map<Long,MemcachedNode> map = ring.asMap();

        assertEquals(5,map.size());

        assertEquals(node2,map.get(5l));
        assertEquals(node1,map.get(1l));

    }

    @Test
    public void testReadOnlyClone() {
        Map<Long,MemcachedNode> nodes = new HashMap<Long,MemcachedNode>();
        List<MemcachedNode> nodesRepresented = new ArrayList(2);

        MemcachedNode node1 = new RingTestingMemcachedNode(1234);
        MemcachedNode node2 = new RingTestingMemcachedNode(-100234);

        nodesRepresented.add(node1);
        nodesRepresented.add(node2);
        // 1, 2, 8, 10, 10, 12, 19, 21, 66, 67 };
        nodes.put(1l,node1);
        nodes.put(2l,node1);
        nodes.put(3l,node1);
        nodes.put(4l,node1);
        nodes.put(5l,node2);

        ArrayBasedCeilRing ring = new ArrayBasedCeilRing(nodes,nodesRepresented);

        ArrayBasedCeilRing ring2 = ring.roClone();

        Map<Long,MemcachedNode> map = ring2.asMap();

        assertEquals(5,map.size());

        assertNotEquals(node2, map.get(5l));
        assertNotEquals(node1, map.get(1l));

        MemcachedNode node = ring2.findClosestNode(6l);
        assertEquals(1234,node.getBytesRemainingToWrite());

    }

    class RingTestingMemcachedNode implements MemcachedNode {


        private final long lastReadDelta;
        public RingTestingMemcachedNode(long lastReadDelta) {
            this.lastReadDelta = lastReadDelta;
        }

        @Override
        public void copyInputQueue() {

        }

        @Override
        public Collection<Operation> destroyInputQueue() {
            return null;
        }

        @Override
        public void setupResend() {

        }

        @Override
        public void fillWriteBuffer(boolean optimizeGets) {

        }

        @Override
        public void transitionWriteItem() {

        }

        @Override
        public Operation getCurrentReadOp() {
            return null;
        }

        @Override
        public Operation removeCurrentReadOp() {
            return null;
        }

        @Override
        public Operation getCurrentWriteOp() {
            return null;
        }

        @Override
        public Operation removeCurrentWriteOp() {
            return null;
        }

        @Override
        public boolean hasReadOp() {
            return false;
        }

        @Override
        public boolean hasWriteOp() {
            return false;
        }

        @Override
        public void addOp(Operation op) {

        }

        @Override
        public void insertOp(Operation o) {

        }

        @Override
        public int getSelectionOps() {
            return 0;
        }

        @Override
        public ByteBuffer getRbuf() {
            return null;
        }

        @Override
        public ByteBuffer getWbuf() {
            return null;
        }

        @Override
        public SocketAddress getSocketAddress() {
            return null;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public long lastReadDelta() {
            return lastReadDelta;
        }

        @Override
        public void completedRead() {

        }

        @Override
        public void reconnecting() {

        }

        @Override
        public void connected() {

        }

        @Override
        public int getReconnectCount() {
            return 0;
        }

        @Override
        public void registerChannel(SocketChannel ch, SelectionKey selectionKey) {

        }

        @Override
        public void setChannel(SocketChannel to) {

        }

        @Override
        public SocketChannel getChannel() {
            return null;
        }

        @Override
        public void setSk(SelectionKey to) {

        }

        @Override
        public SelectionKey getSk() {
            return null;
        }

        @Override
        public int getBytesRemainingToWrite() {
            return (int)lastReadDelta;
        }

        @Override
        public int writeSome() throws IOException {
            return 0;
        }

        @Override
        public void fixupOps() {

        }

        @Override
        public void authComplete() {

        }

        @Override
        public void setupForAuth() {

        }

        @Override
        public void setContinuousTimeout(boolean timedOut) {

        }

        @Override
        public int getContinuousTimeout() {
            return 0;
        }

        @Override
        public MemcachedConnection getConnection() {
            return null;
        }

        @Override
        public void setConnection(MemcachedConnection connection) {

        }
    }
}
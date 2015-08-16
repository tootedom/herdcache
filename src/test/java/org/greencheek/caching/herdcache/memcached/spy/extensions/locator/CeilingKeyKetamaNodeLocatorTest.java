package org.greencheek.caching.herdcache.memcached.spy.extensions.locator;

import net.spy.memcached.*;
import net.spy.memcached.ops.Operation;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CeilingKeyKetamaNodeLocatorTest {
    private static int NUM = 1000000;
    private static int NUM_NODES = 1000;
    private static List<String> strings;
    private static List<MemcachedNode> memcachedNodes;
    static {
        List<String> randoms = new ArrayList<>(NUM);
        for(int i=0;i<NUM;i++) {
            randoms.add(UUID.randomUUID().toString());
        }

        strings = new ArrayList<>(randoms);

        memcachedNodes = new ArrayList<>(NUM_NODES);

        for(int i=0;i<NUM_NODES;i++) {
            memcachedNodes.add(new KetemaTestingMemcachedNode(i));
        }
    }


    private NodeLocator original = new KetamaNodeLocator(memcachedNodes, new JenkinsHash());
    private NodeLocator customarray = new CeilingKeyKetamaNodeLocator(memcachedNodes, new JenkinsHash());

    @Test
    public void testSameNodesAreRoutedTo() {
        List<MemcachedNode> originalList = new ArrayList<>(NUM+1);
        for(int i=0;i<NUM;i++) {
            originalList.add(original.getPrimary(strings.get(i)));
        }

        List<MemcachedNode> customList = new ArrayList<>(NUM+1);
        for(int i=0;i<NUM;i++) {
            customList.add(customarray.getPrimary(strings.get(i)));
        }

        assertArrayEquals(originalList.toArray(),customList.toArray());
    }





    static class KetemaTestingMemcachedNode implements MemcachedNode {


        private final long lastReadDelta;
        public KetemaTestingMemcachedNode(long lastReadDelta) {
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
            return new InetSocketAddress("127.0.0.1",(int)lastReadDelta);
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
            return 0;
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

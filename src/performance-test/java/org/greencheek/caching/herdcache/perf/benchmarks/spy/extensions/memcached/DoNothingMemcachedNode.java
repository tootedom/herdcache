package org.greencheek.caching.herdcache.perf.benchmarks.spy.extensions.memcached;

import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;

/**
 * Created by dominictootell on 12/08/2015.
 */
public class DoNothingMemcachedNode implements MemcachedNode {


    private final SocketAddress address;

    public DoNothingMemcachedNode(int port) {
        this.address = new InetSocketAddress("127.0.0.1",port);
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
        return address;
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
        return 0;
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

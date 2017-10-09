package org.greencheek.caching.herdcache.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 09/10/2017.
 */
public class EmptyBlockingQueue<T> implements BlockingQueue<T> {
    public boolean add(T e) {
        throw new IllegalStateException();
    }

    public boolean offer(T e) {
        return false;
    }

    public void put(T e) throws InterruptedException {
        return;
    }

    public boolean offer(T e, long l, TimeUnit tu) throws InterruptedException {
        tu.sleep(l);
        return false;
    }

    public T take() throws InterruptedException {
        throw new InterruptedException();
    }

    public T poll(long l, TimeUnit tu) throws InterruptedException {
        tu.sleep(l);
        return null;
    }

    public int remainingCapacity() {
        return 0;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean contains(Object o) {
        return false;
    }

    public int drainTo(Collection<? super T> clctn) {
        return 0;
    }

    public int drainTo(Collection<? super T> clctn, int i) {
        return 0;
    }

    public T remove() {
        return null;
    }

    public T poll() {
        return null;
    }

    public T element() {
        return null;
    }

    public T peek() {
        return null;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return false;
    }

    public Iterator<T> iterator() {
        return null;
    }

    public Object[] toArray() {
        return null;
    }

    public <T> T[] toArray(T[] ts) {
        return null;
    }

    public boolean containsAll(Collection<?> clctn) {
        return false;
    }

    public boolean addAll(Collection<? extends T> clctn) {
        return false;
    }

    public boolean removeAll(Collection<?> clctn) {
        return false;
    }

    public boolean retainAll(Collection<?> clctn) {
        return false;
    }

    public void clear() {
    }
}

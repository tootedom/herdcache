package org.greencheek.caching.herdcache.memcached.predicates;

import org.greencheek.caching.herdcache.IsCachedValueUsable;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * An implementation of {@link java.util.function.Predicate} that stores a reference to the object
 * that was tested.  This is so that the owner of the Predicate can obtain the value at a later point.
 *
 * for example the predicate passed to the constructor might have decided that the object retrieved from
 * cache was too old, and therefore not useable.  However, the {@link java.util.function.Supplier} that is
 * your backend call failed, and as a result returned null.
 *
 * You now have the opportunity to obtain the tested cached item and use it as a fallback (stale) content.
 */
public class StatefulPredicate<V extends Serializable> implements Predicate<V>, IsCachedValueUsable<V> {
    private volatile V testedObject;
    private final Predicate<V> wrappedPredicate;

    public StatefulPredicate(Predicate<V> testingPredicate) {
        this.wrappedPredicate = testingPredicate;
    }
    public boolean test(V itemToTest) {
        testedObject = itemToTest;
        return wrappedPredicate.test(itemToTest);
    }

    public V getTestedObject() {
        return testedObject;
    }

}

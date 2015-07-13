package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.domain.CachedItemPredicate;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The getAndSet methods will always set the expiry of the object in the cache to an infinite TTL.
 * In the case of memcached, this is 0.  The only time the item will if an item needs to be evicted from the cache
 *
 * As a result, the cachedItemAgeInMillis isn't used at insertion time, but after the item has been fetched.  The ttl
 * is then used to determine if the object is valid.
 *
 * When a value is generated by the {@link java.util.function.Supplier} backend function,
 * it is wrapped in a {@link org.greencheek.caching.herdcache.domain.CachedItem}
 *
 * If no TTL is specified, then the given Predicate is called with the cached object to allow the caller to determine
 * if the cached item is valid.
 *
 * Cache whose item's expiry is governed by a combination of factors:
 *
 * <ul>
 *     <li>The ttl (in millis, or a Duration), of the cache item</li>
 *     <li>A custom Predicate that is passed the cached object, and can evaluate the object as the called sees fit</li>
 *     <li>A custom Predicate of type @link #CachedItemPredicate from which The predicate can obtain the cached object and
 *     also an Instant representing the time (UTC) the item was cached</li>
 * </ul>
 *
 * You should <b>NOT</b> mix the calls to {@link org.greencheek.caching.herdcache.CacheWithExpiry#apply} and
 * the calls to any <b>getOrSet</b> methods on {@link SerializableOnlyCacheWithUserSuppliedExpiry}
 */
public interface SerializableOnlyCacheWithUserSuppliedExpiry<V extends Serializable> extends AwaitOnFuture<V> {

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * Any value that is stored in the cache is valid for use.
     * Any value returned by the supplier (theBackEndCall)
     *
     * theBackEndCall is executed on the caller thread
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     *
     * @return A future representing either a previous lookup up for the key, or the execution of the supplier function.
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall) {
        return getOrSet(key,theBackEndCall,0,IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE,
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,MoreExecutors.newDirectExecutorService());
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * Any value that is stored in the cache is valid for use.
     * Any value returned by the supplier (theBackEndCall)
     *
     * theBackEndCall is executed on the given executor
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param executor The {@link com.google.common.util.concurrent.ListeningExecutorService} on which to execute theBackEndCall. only the
     *                 execution of the Supplier function is executed on the executor.  The cache get is executed on the caller thread (i.e.
     *                 the thread the method was called from)
     * @return A future representing either a previous lookup up for the key, or the execution of the supplier function.
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall,ListeningExecutorService executor ) {
        return getOrSet(key,theBackEndCall,0,IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE,
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,executor);
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * theBackEndCall is executed on the given executor
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable A {@link java.util.function.Predicate} that determines if the value
     *                               returned by theBackEndCall supplier function should be stored in the cache or not
     * @param executor The {@link com.google.common.util.concurrent.ListeningExecutorService} on which to execute theBackEndCall. only the
     *                 execution of the Supplier function is executed on the executor.  The cache get is executed on the caller thread (i.e.
     *                 the thread the method was called from)
     * @return A future representing either a previous lookup up for the key, or the execution of the supplier function.
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall,IsCachedValueUsable<V> isCachedValueUsable,IsSupplierValueCachable<V> isSupplierValueCachable,ListeningExecutorService executor ) {
        return getOrSet(key, theBackEndCall, 0, isCachedValueUsable, isSupplierValueCachable, executor);
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method)
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable A {@link java.util.function.Predicate} that determines if the value
     *                               returned by theBackEndCall supplier function should be stored in the cache or not
    *
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall,IsCachedValueUsable<V> isCachedValueUsable,IsSupplierValueCachable<V> isSupplierValueCachable ) {
        return getOrSet(key, theBackEndCall, 0, isCachedValueUsable, isSupplierValueCachable, MoreExecutors.newDirectExecutorService());
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method), and the returned
     * value can always be stored in the cache
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall,IsCachedValueUsable<V> isCachedValueUsable ) {
        return getOrSet(key, theBackEndCall, 0, isCachedValueUsable, IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE, MoreExecutors.newDirectExecutorService());
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can always be stored in the cache.
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem} creation
     * date is checked to see if it's time to live, according to the passed Duration, has expired.  If the item is
     * not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid.
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param cachedItemAgeInMillis  The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem}.
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, Duration cachedItemAgeInMillis) {
        return getOrSet(key, theBackEndCall, cachedItemAgeInMillis.toMillis());
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The cached value returned, or the cache value generated by the supplier, is always valid
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can always be stored in the cache.
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem} creation
     * date is checked to see if it's time to live, according to the passed Duration, has expired.  If the item is
     * not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid.
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param cachedItemAgeInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} in millis
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, long cachedItemAgeInMillis) {
        return getOrSet(key, theBackEndCall, cachedItemAgeInMillis, IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE,
                        IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE, MoreExecutors.newDirectExecutorService());
    }

    /**
     *
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can always be stored in the cache.
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem} creation
     * date is checked to see if it's time to live, according to the passed Duration, has expired.  If the item is
     * not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid.
     *
     *
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param cachedItemAgeInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} in millis
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, Duration cachedItemAgeInMillis, IsCachedValueUsable<V> isCachedValueUsable) {
        return getOrSet(key, theBackEndCall, cachedItemAgeInMillis.toMillis(), isCachedValueUsable);
    }

    /**
     *
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can always be stored in the cache ({@link Cache#CAN_ALWAYS_CACHE_VALUE}
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem} creation
     * date is checked to see if it's time to live, according to the passed Duration (specified in millis), has expired.
     * If the item is not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid.
     *
     *
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param cachedItemAgeInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} in millis
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, long cachedItemAgeInMillis, IsCachedValueUsable<V> isCachedValueUsable) {
        return getOrSet(key, theBackEndCall, cachedItemAgeInMillis, isCachedValueUsable,IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE, MoreExecutors.newDirectExecutorService());
    }

    /**
     *
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can only be cached is the given {@link java.util.function.Predicate} confirms that the generated item is
     * cachable.
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem} creation
     * date is checked to see if it's time to live, according to the passed Duration (specified in millis), has expired.
     * If the item is not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid.
     *
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param cachedItemAgeInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} in millis
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and determines if it can be cached or
     *                               not
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, Duration cachedItemAgeInMillis,
                                                IsCachedValueUsable<V> isCachedValueUsable, IsSupplierValueCachable<V> isSupplierValueCachable) {
        return getOrSet(key, theBackEndCall, cachedItemAgeInMillis.toMillis(), isCachedValueUsable, isSupplierValueCachable, MoreExecutors.newDirectExecutorService());
    }

    /**
     *
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can only be cached is the given {@link java.util.function.Predicate} confirms that the generated item is
     * cachable.
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem} creation
     * date is checked to see if it's time to live, according to the passed Duration (specified in millis), has expired.
     * If the item is not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid.
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param cachedItemAgeInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} in millis
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and determines if it can be cached or
     *                               not
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, long cachedItemAgeInMillis,
                                                IsCachedValueUsable<V> isCachedValueUsable, IsSupplierValueCachable<V> isSupplierValueCachable) {
        return getOrSet(key, theBackEndCall, cachedItemAgeInMillis, isCachedValueUsable, isSupplierValueCachable, MoreExecutors.newDirectExecutorService());
    }

    /**
     *
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (backEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can always be stored in the cache ({@link Cache#CAN_ALWAYS_CACHE_VALUE}
     *
     * The suppliers execute on a the executor service that is passed to the method
     *
     * When an item is retrieved from the cache the {@link org.greencheek.caching.herdcache.domain.CachedItem}
     * If the item is not a {@link org.greencheek.caching.herdcache.domain.CachedItem} then it is valid to be used.
     * If it is a CachedItem, then if  is passed to the instance of the predeciate: {@link org.greencheek.caching.herdcache.domain.CachedItemPredicate}
     *
     *
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param executor The passed in executor to execute the supplier function on
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, CachedItemPredicate<V> isCachedValueUsable,
                                                ListeningExecutorService executor) {
        return getOrSet(key, theBackEndCall, isCachedValueUsable, IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE, executor);
    }

    /**
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param isCachedValueUsable A {@link org.greencheek.caching.herdcache.IsCachedValueUsable} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and it determines if the item that has been
     *                                created by {@code theBackEndCall} can be cached or not
     * @param executor
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, CachedItemPredicate<V> isCachedValueUsable,
                                                IsSupplierValueCachable<V> isSupplierValueCachable, ListeningExecutorService executor) {
        return getOrSet(key, theBackEndCall,(IsCachedValueUsable)isCachedValueUsable, isSupplierValueCachable, executor);
    }


    /**
     *
     * @param key The key to look in the cache for, or store the value from theBackEndCall
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param getInspectionTTLInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} as a {@link java.time.Duration}
     *
     * @param isCachedValueUsable A {@link org.greencheek.caching.herdcache.IsCachedValueUsable} that determines if the value
     *                          retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and it determines if the item that has been
     *                                created by {@code theBackEndCall} can be cached or not
     * @param executor
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, Duration getInspectionTTLInMillis, IsCachedValueUsable<V> isCachedValueUsable,
                                                IsSupplierValueCachable<V> isSupplierValueCachable, ListeningExecutorService executor) {
        return getOrSet(key,theBackEndCall,getInspectionTTLInMillis.toMillis(),isCachedValueUsable,isSupplierValueCachable,executor);
    }
    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (theBackEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can only be cached is the given {@link java.util.function.Predicate} confirms that the generated item is
     * cachable.  The supplier (theBackEndCall) is executed on the given executor
     *
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param getInspectionTTLInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} in millis
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                            retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and determines if it can be cached or
     *                               not
     * @param executor
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, long getInspectionTTLInMillis, IsCachedValueUsable<V> isCachedValueUsable,
                                                IsSupplierValueCachable<V> isSupplierValueCachable, ListeningExecutorService executor) {
        return getOrSet(key, theBackEndCall, getInspectionTTLInMillis, (Predicate<V>)isCachedValueUsable,
                (Predicate<V>)isSupplierValueCachable, executor);
    }


    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The canUseCachedValue cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (theBackEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can only be cached is the given {@link java.util.function.Predicate} confirms that the generated item is
     * cachable.  The supplier (theBackEndCall) is executed on the given executor
     *
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param getInspectionTTLInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} as a {@link java.time.Duration}
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                            retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and determines if it can be cached or
     *                               not
     * @param executor
     * @return
     */
    default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, Duration getInspectionTTLInMillis, Predicate<V> isCachedValueUsable,
                                        Predicate<V> isSupplierValueCachable, ListeningExecutorService executor) {
        return getOrSet(key, theBackEndCall, getInspectionTTLInMillis.toMillis(),  isCachedValueUsable,
                isSupplierValueCachable, executor);
    }

    /**
     * Gets an item from the cache, or calls the backEnd (the supplier Supplier function) to generate
     * a value that is stored in the cache.  The item is added to the cache without an expiry.
     *
     * The isCachedValueUsable cache value predicate is used to determine if the value, if obtained from the cache,
     * can be used or not.
     *
     * The supplier (theBackEndCall) is executed on the caller thread (same thread that called the method),and the returned
     * value can only be cached is the given {@link java.util.function.Predicate} confirms that the generated item is
     * cachable.  The supplier (theBackEndCall) is executed on the given executor
     *
     * @param theBackEndCall The {@link java.util.function.Supplier} that generates the
     *                       value that should be cached against the key
     * @param getInspectionTTLInMillis The max age of a {@link org.greencheek.caching.herdcache.domain.CachedItem} as a long in millis
     * @param isCachedValueUsable A {@link java.util.function.Predicate} that determines if the value
     *                            retrieve from the cache can be used, or if theBackEndCall should be executed for a value
     * @param isSupplierValueCachable This predicate is passed the generated item, and determines if it can be cached or
     *                               not
     * @param executor
     * @return
     */
    public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall, long getInspectionTTLInMillis, Predicate<V> isCachedValueUsable,
                                        Predicate<V> isSupplierValueCachable, ListeningExecutorService executor);




    default public ListenableFuture<V> get(String key) {
        return get(key, MoreExecutors.newDirectExecutorService());
    }

    public ListenableFuture<V> get(String key,ListeningExecutorService executorService);


}

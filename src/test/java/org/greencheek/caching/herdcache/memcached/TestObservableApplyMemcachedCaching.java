package org.greencheek.caching.herdcache.memcached;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.hystrix.HystrixCommand;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.exceptions.UnableToScheduleCacheGetExecutionException;
import org.greencheek.caching.herdcache.exceptions.UnableToSubmitSupplierForExecutionException;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.metrics.YammerMetricsRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.FastSerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.SerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.util.BackEndRequest;
import org.greencheek.caching.herdcache.util.Content;
import org.greencheek.caching.herdcache.util.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Single;
import rx.schedulers.Schedulers;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestObservableApplyMemcachedCaching {
    private String largeCacheValue = "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called “cowboy entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then “hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called “cowboy entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then “hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called “cowboy entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then “hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called “cowboy entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called \"cowboy\" entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then “hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called \"cowboy\" entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then “hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called \"cowboy\" entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then \"hook into\". As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called “cowboy entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then \"hook into\". As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" +
            "\n" +
            "# Memcached For Spray #\n" +
            "\n" +
            "This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides\n" +
            "a memcached backed cache for spray caching.\n" +
            "\n" +
            "## Overview ##\n" +
            "\n" +
            "Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of\n" +
            "keys to executing futures.\n" +
            "\n" +
            "When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.\n" +
            "At this point the entry is removed from the internal cache; and will only exist in memcached.\n" +
            "\n" +
            "Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is\n" +
            "returned.\n" +
            "\n" +
            "The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized\n" +
            "objects.\n" +
            "\n" +
            "\n" +
            "## Dependencies ##\n" +
            "\n" +
            "The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.\n" +
            "\n" +
            "## Thundering Herd ##\n" +
            "\n" +
            "The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),\n" +
            "store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may\n" +
            "not be completed, and is specific to the client that generated the future.\n" +
            "\n" +
            "This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):\n" +
            "\n" +
            "    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a\n" +
            "    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally\n" +
            "    (without special guarding techniques, like so-called \"cowboy\" entries) this can cause many requests\n" +
            "    to compete for system resources while trying to compute the same result thereby greatly reducing overall\n" +
            "    system performance. When you use a spray-caching cache the very first request that arrives for a certain\n" +
            "    cache key causes a future to be put into the cache which all later requests then “hook into. As soon as\n" +
            "    the first request completes all other ones complete as well. This minimizes processing time and server\n" +
            "    load for all requests.\n" +
            "\n" +
            "Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each\n" +
            "request compute the value, all the requests wait on the 1 invocation of the value to be computed.\n" +
            "\n" +
            "### Memcached thundering herd, how does it do it  ###\n" +
            "\n" +
            "There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)\n" +
            "and the memcached library implementation.\n" +
            "\n" + UUID.randomUUID().toString();

    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private ObservableCache cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

        memcached = MemcachedDaemonFactory.createMemcachedDaemon(false);

        if (memcached.getDaemon() == null) {
            throw new RuntimeException("Unable to start local memcached");
        }


    }

    @After
    public void tearDown() {
        if (memcached != null) {
            memcached.getDaemon().stop();
        }

        if (cache != null && cache instanceof RequiresShutdown) {
            ((RequiresShutdown) cache).shutdown();
        }

        executorService.shutdownNow();
    }

//    private void testHashAlgorithm(HashAlgorithm algo) {
//        cache = new SpyMemcachedCache<String>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setHashAlgorithm(algo)
//                        .setKeyPrefix(Optional.of("elastic"))
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testJenkinsHashAlgorithm() {
//        testHashAlgorithm(new JenkinsHash());
//    }
//
//
//    @Test
//    public void testXXHashAlgorithm() {
//        testHashAlgorithm(new XXHashAlogrithm());
//    }
//
//    @Test
//    public void testAsciiXXHashAlgorithm() {
//        testHashAlgorithm(new AsciiXXHashAlogrithm());
//    }

    @Test
    public void testMemcachedCache() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger cacheApplyInvocations = new AtomicInteger(0);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cacheApplyInvocations.incrementAndGet();
            return "value1";
        },Duration.ZERO);


        System.out.println(Thread.currentThread().getName());
        val.subscribeOn(Schedulers.io())

        .observeOn(Schedulers.immediate())
        .subscribe(value -> {
            System.out.println("sub1: " + Thread.currentThread().getName());

            try {
                assertEquals("Value should be key1", "value1", value.getValue().get());
            } finally {
                latch.countDown();
            }
            cacheApplyInvocations.incrementAndGet();

        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cacheApplyInvocations.incrementAndGet();
            return "value2";
        }, Duration.ZERO);


        System.out.println(val);
        System.out.println(val2);

        val2.observeOn(Schedulers.immediate());
        val2.subscribe(value -> {
            System.out.println("sub2: " + Thread.currentThread().getName());
            try {
                assertEquals("Value should be key1", "value1", value.getValue().get());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        assertEquals(2, cacheApplyInvocations.get());
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());


    }

//    @Test
//    public void testNoCacheKeyHashingMemcachedCache() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.NONE)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testDifferenceCachedItemTimes() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setSetWaitDuration(Duration.ofSeconds(3))
//                        .setKeyHashType(KeyHashingType.NONE)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            return "value1";
//        }, Duration.ofSeconds(3), executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key2", () -> {
//            return "value2";
//        }, Duration.ofSeconds(1), executorService);
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        ListenableFuture<String> val1_again = cache.apply("Key1", () -> {
//            return "value1_again";
//        }, Duration.ofSeconds(3), executorService);
//
//        ListenableFuture<String> val2_again = cache.apply("Key2", () -> {
//            return "value2_again";
//        }, Duration.ofSeconds(1), executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val1_again, null));
//        assertEquals("Value should be key2", "value2_again", cache.awaitForFutureOrElse(val2_again, null));
//
//        assertEquals(2, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//
//    @Test
//    public void testMD5LowerKeyHashingMemcachedCache() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_LOWER)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testMD5LowerKeyHashingMemcachedCacheWithCachePrefix() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_LOWER)
//                        .setKeyPrefix(Optional.of("bob"))
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//    }
//
//    @Test
//    public void testMD5LowerKeyHashingMemcachedCacheWithNoHashingOfCacheKeyPrefix() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_LOWER)
//                        .setKeyPrefix(Optional.of("bob"))
//                        .setHashKeyPrefix(false)
//                        .setAsciiOnlyKeys(true)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//    }
//
//    @Test
//    public void testSHA256LowerKeyHashingMemcachedCache() {
//        cache = new SpyMemcachedCache<String>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.SHA256_LOWER)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testCachePredicate() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_UPPER)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService, (value) -> {
//            return !value.equals("value1");
//        });
//
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value for key1 should be value1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value for key1 should be value2", "value2", cache.awaitForFutureOrElse(val2, null));
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//
//        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value3";
//        }, executorService);
//
//        assertEquals("Value for key1 should be value2", "value2", cache.awaitForFutureOrElse(val3, null));
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testMD5UpperKeyHashingMemcachedCache() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_UPPER)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    public static class Document implements Serializable {
//        static final long serialVersionUID = 42L;
//
//        private final String title;
//        private final String author;
//        private final String content;
//
//        public Document(String title, String author, String content) {
//            this.title = title;
//            this.author = author;
//            this.content = content;
//        }
//
//        public boolean equals(Object o) {
//            if (o instanceof Document) {
//                Document other = (Document) o;
//
//                if (other.title.equals(this.title) &&
//                        other.author.equals(this.author) &&
//                        other.content.equals(this.content)) {
//                    return true;
//                } else {
//                    return false;
//                }
//            } else {
//                return false;
//            }
//        }
//    }
//
//    @Test
//    public void testSerializationInMemcachedCache() {
//        cache = new SpyMemcachedCache<Document>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_UPPER)
//                        .setSerializingTranscoder(new SerializingTranscoder())
//                        .buildMemcachedConfig()
//        );
//
//        Document nemo = new Document("Finding Nemo", "Disney", largeCacheValue);
//        Document jungle = new Document("Jungle Book", "Disney", largeCacheValue);
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return nemo;
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return jungle;
//        }, executorService);
//
//
//        assertEquals("Value should be key1", nemo, cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", nemo, cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testFastSerializationInMemcachedCache() {
//        cache = new SpyMemcachedCache<Document>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_UPPER)
//                        .setSerializingTranscoder(new FastSerializingTranscoder())
//                        .buildMemcachedConfig()
//        );
//
//        Document nemo = new Document("Finding Nemo", "Disney", largeCacheValue);
//        Document jungle = new Document("Jungle Book", "Disney", largeCacheValue);
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return nemo;
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return jungle;
//        }, executorService);
//
//
//        assertEquals("Value should be nemo object", nemo, cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be nemo object", nemo, cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals("Value should be nemo object", nemo, cache.awaitForFutureOrElse(cache.get("Key1"), null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testLargeCacheByteValue() {
//        MetricRegistry registry = new MetricRegistry();
//
//        byte[] largeCacheValueAsBytes = largeCacheValue.getBytes();
//
//        cache = new SpyMemcachedCache<byte[]>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_LOWER)
//                        .setSerializingTranscoder(new SerializingTranscoder(Integer.MAX_VALUE))
//                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
//                        .buildMemcachedConfig()
//        );
//
//        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build();
//
//        try {
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return largeCacheValueAsBytes;
//            }, executorService);
//
//            ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "value2".getBytes();
//            }, executorService);
//
//
//            assertEquals("Value should be key1", largeCacheValueAsBytes, cache.awaitForFutureOrElse(val, null));
//            assertEquals("Value should be key1", largeCacheValueAsBytes, cache.awaitForFutureOrElse(val2, null));
//
//            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//        } finally {
//            reporter.report();
//            reporter.stop();
//        }
//    }
//
//
//    @Test
//    public void testLargeCacheValue() {
//
//
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.MD5_LOWER)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return largeCacheValue;
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", largeCacheValue, cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", largeCacheValue, cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//
//    @Test
//    public void testRejectedExecutionIsRemovedFromCache() {
//
//        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
//                0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<Runnable>(1)));
//        try {
//            cache = new SpyMemcachedCache<>(
//                    new ElastiCacheCacheConfigBuilder()
//                            .setMemcachedHosts("localhost:" + memcached.getPort())
//                            .setTimeToLive(Duration.ofSeconds(60))
//                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                            .setWaitForMemcachedSet(true)
//                            .setKeyHashType(KeyHashingType.MD5_LOWER)
//                            .buildMemcachedConfig()
//            );
//
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return largeCacheValue;
//            }, executorService);
//
//            ListenableFuture<String> val2 = cache.apply("Key2", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "value2";
//            }, executorService);
//
//
//            ListenableFuture<String> val3 = cache.apply("Key3", () -> "value3", executorService);
//            assertEquals("Value should for key1 should be largeCacheValue", largeCacheValue, cache.awaitForFutureOrElse(val, null));
//            assertEquals("Value should for key2 should be value2", "value2", cache.awaitForFutureOrElse(val2, null));
//            assertEquals("Value should for key3 should be null", null, cache.awaitForFutureOrElse(val3, null));
//
//            ListenableFuture<String> val3again = cache.apply("Key3", () -> "value3", executorService);
//            assertEquals("Value should for key3 should be value3", "value3", cache.awaitForFutureOrElse(val3again,"exception", "timeout",1000,TimeUnit.MILLISECONDS));
//
//            assertEquals(3, memcached.getDaemon().getCache().getCurrentItems());
//        }
//        finally {
//            executorService.shutdownNow();
//        }
//    }
//
//
//    @Test(expected=UnableToSubmitSupplierForExecutionException.class)
//    public void testRejectedExecutionThrowsException() throws Throwable {
//
//        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
//                0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<Runnable>(1)));
//        try {
//            cache = new SpyMemcachedCache<>(
//                    new ElastiCacheCacheConfigBuilder()
//                            .setMemcachedHosts("localhost:" + memcached.getPort())
//                            .setTimeToLive(Duration.ofSeconds(60))
//                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                            .setWaitForMemcachedSet(true)
//                            .setKeyHashType(KeyHashingType.MD5_LOWER)
//                            .buildMemcachedConfig()
//            );
//
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return largeCacheValue;
//            }, executorService);
//
//            ListenableFuture<String> val2 = cache.apply("Key2", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "value2";
//            }, executorService);
//
//
//            ListenableFuture<String> val3 = cache.apply("Key3", () -> "value3", executorService);
//            assertEquals("Value should for key1 should be largeCacheValue", largeCacheValue, cache.awaitForFutureOrElse(val, null));
//            assertEquals("Value should for key2 should be value2", "value2", cache.awaitForFutureOrElse(val2, null));
//            assertEquals("Value should for key3 should be null", null, val3.get());
//        } catch (Exception e) {
//            throw e.getCause();
//        } finally {
//            executorService.shutdownNow();
//        }
//    }
//
//
//    public static class ApplicationException extends RuntimeException {
//
//    }
//
//    @Test(expected=ApplicationException.class)
//    public void testApplicationExceptionIsPropagatedToCaller() throws Throwable {
//        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
//                0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<Runnable>(1)));
//        try {
//            cache = new SpyMemcachedCache<>(
//                    new ElastiCacheCacheConfigBuilder()
//                            .setMemcachedHosts("localhost:" + memcached.getPort())
//                            .setTimeToLive(Duration.ofSeconds(60))
//                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                            .setWaitForMemcachedSet(true)
//                            .setKeyHashType(KeyHashingType.MD5_LOWER)
//                            .buildMemcachedConfig()
//            );
//
//            // Submit an item to the cache "Key1" is executing
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                throw new ApplicationException();
//            }, executorService);
//
//            assertEquals("Value should for key1 should be largeCacheValue", largeCacheValue, val.get());
//        } catch (Exception e) {
//            throw e.getCause();
//        } finally {
//            executorService.shutdownNow();
//        }
//    }
//
//    @Test(expected=UnableToScheduleCacheGetExecutionException.class)
//    public void testRejectedExecutionInGetThrowsException() throws Throwable {
//
//        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
//                0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<Runnable>(1)));
//        try {
//            cache = new SpyMemcachedCache<>(
//                    new ElastiCacheCacheConfigBuilder()
//                            .setMemcachedHosts("localhost:" + memcached.getPort())
//                            .setTimeToLive(Duration.ofSeconds(60))
//                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                            .setWaitForMemcachedSet(true)
//                            .setKeyHashType(KeyHashingType.MD5_LOWER)
//                            .buildMemcachedConfig()
//            );
//
//            // Submit an item to the cache "Key1" is executing
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return largeCacheValue;
//            }, executorService);
//
//            // Submit an item to the cache "Key2" is queued
//            ListenableFuture<String> val2 = cache.apply("Key2", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "value2";
//            }, executorService);
//
//
//            // Submit a GET, this will be rejected as Key1 is executing, the Key2 is filling the queue
//            // The rejection is set on the ListenableFuture, so when you do to .get() it will throw an exception.
//            ListenableFuture<String> val3 = cache.get("Key3", executorService);
//
//            assertEquals("Value should for key1 should be largeCacheValue", largeCacheValue, cache.awaitForFutureOrElse(val, null));
//            assertEquals("Value should for key2 should be value2", "value2", cache.awaitForFutureOrElse(val2, null));
//
//            // Throws the excpetion
//            assertEquals("Value should for key3 should be null", null, val3.get());
//        } catch (Exception e) {
//            throw e.getCause();
//        } finally {
//            executorService.shutdownNow();
//        }
//    }
//
//    @Test
//    public void testSHA256UpperKeyHashingMemcachedCache() {
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.SHA256_UPPER)
//                        .buildMemcachedConfig()
//        );
//
//        ListenableFuture<String> val = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value1";
//        }, executorService);
//
//        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "value2";
//        }, executorService);
//
//
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//    }
//
//    @Test
//    public void testYammerMetricsRecording() {
//        MetricRegistry registry = new MetricRegistry();
//
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.SHA256_UPPER)
//                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
//                        .buildMemcachedConfig()
//        );
//
//        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build();
//
//        try {
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "value1";
//            }, executorService);
//
//            ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "value2";
//            }, executorService);
//
//
//            assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
//            assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));
//
//            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//            Set<String> metricNames = registry.getNames();
//            assertTrue(metricNames.size() > 0);
//
//            assertTrue(metricNames.containsAll(new ArrayList() {{
//                add("distributed_cache_count");
//                add("distributed_cache_misscount");
//                add("distributed_cache_missrate");
//                add("distributed_cache_timer");
//                add("distributed_cache_writes_count");
//                add("value_calculation_cache_hitcount");
//                add("value_calculation_cache_hitrate");
//                add("value_calculation_success_count");
//                add("value_calculation_time_timer");
//            }}));
//
//        } finally {
//            reporter.report();
//            reporter.stop();
//        }
//    }
//
//    @Test
//    public void testDoNotUseCachedValue() {
//        MetricRegistry registry = new MetricRegistry();
//
//        Predicate<String> cachedValueAllowed = (String value) -> {
//            return value.equals("cacheable");
//        };
//
//        cache = new SpyMemcachedCache<>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.SHA256_UPPER)
//                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
//                        .buildMemcachedConfig()
//        );
//
//        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build();
//
//        try {
//            ListenableFuture<String> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "not_cacheable";
//            }, executorService, Cache.CAN_ALWAYS_CACHE_VALUE, cachedValueAllowed);
//
//
//            assertEquals("Value should be key1", "not_cacheable", cache.awaitForFutureOrElse(val, null));
//
//            ListenableFuture<String> val2 = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "someothervalue";
//            }, executorService, (x) -> false, cachedValueAllowed);
//
//
//            assertEquals("Value should be key1", "someothervalue", cache.awaitForFutureOrElse(val2, null));
//
//            ListenableFuture<String> val3 = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "someothervalue";
//            }, executorService, Cache.CAN_ALWAYS_CACHE_VALUE);
//
//
//            assertEquals("Value should be key1", "not_cacheable", cache.awaitForFutureOrElse(val3, null));
//
//
//            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//            Set<String> metricNames = registry.getNames();
//            assertTrue(metricNames.size() > 0);
//
//        } finally {
//            reporter.report();
//            reporter.stop();
//        }
//    }
//
//    @Test
//    public void testDoNotUseSerialisedCachedValue() {
//        MetricRegistry registry = new MetricRegistry();
//
//        Predicate<Content> cachedValueAllowed  = (Content value) ->
//             value.getCreationDateEpoch() + System.currentTimeMillis() < 500;
//
//
//
//        cache = new SpyMemcachedCache<Content>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.SHA256_UPPER)
//                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
//                        .buildMemcachedConfig()
//        );
//
//        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .build();
//
//        try {
//            ListenableFuture<Content> val = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return new Content("not_cacheable");
//            }, executorService, Cache.CAN_ALWAYS_CACHE_VALUE, cachedValueAllowed);
//
//
//            assertEquals("Value should be key1", "not_cacheable", ((Content) cache.awaitForFutureOrElse(val, null)).getContent());
//
//            ListenableFuture<Content> val2 = cache.apply("Key1", () -> {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return new Content("someothervalue");
//            }, executorService, (x) -> false, cachedValueAllowed);
//
//
//            assertEquals("Value should be key1", "someothervalue", ((Content) cache.awaitForFutureOrElse(val2, null)).getContent());
//
//            ListenableFuture<String> val3 = cache.get("Key1");
//            assertEquals("Value should be key1", "not_cacheable", ((Content) cache.awaitForFutureOrElse(val3, null)).getContent());
//
//
//            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
//
//            Set<String> metricNames = registry.getNames();
//            assertTrue(metricNames.size() > 0);
//        } finally {
//            reporter.report();
//            reporter.stop();
//        }
//
//
//    }
//
//    @Test
//    public void testDoNotUseSerialisedCachedValueAndHystrix() {
//
//        cache = new SpyMemcachedCache<Content>(
//                new ElastiCacheCacheConfigBuilder()
//                        .setMemcachedHosts("localhost:" + memcached.getPort())
//                        .setTimeToLive(Duration.ofSeconds(60))
//                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
//                        .setWaitForMemcachedSet(true)
//                        .setKeyHashType(KeyHashingType.SHA256_UPPER)
//                        .buildMemcachedConfig()
//        );
//
//        RestClient timeoutClient = new RestClient() {
//            @Override
//            public Content get(String key) {
//                try {
//                    Thread.sleep(20000);
//                    return new Content("content");
//                } catch (InterruptedException e) {
//                    throw new RuntimeException();
//                }
//            }
//
//            public String toString() {
//                return "timeoutClient";
//            }
//        };
//
//        RestClient baseClient = new RestClient() {
//            @Override
//            public Content get(String key) {
//                return new Content("content_from_client");
//            }
//
//            public String toString() {
//                return "baseClient";
//            }
//        };
//
//
//        assertEquals("content_from_client", new BackEndRequest("World",baseClient,cache).execute().getContent());
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        HystrixCommand<Content> content = new BackEndRequest("World",timeoutClient,cache);
//
//        assertEquals("content_from_client", content.execute().getContent());
//
//        assertTrue(content.isResponseFromFallback());
//    }
}

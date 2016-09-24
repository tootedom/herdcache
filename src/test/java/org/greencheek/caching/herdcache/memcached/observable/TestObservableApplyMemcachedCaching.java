package org.greencheek.caching.herdcache.memcached.observable;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.hystrix.HystrixCommand;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.memcached.SpyObservableMemcachedCache;
import org.greencheek.caching.herdcache.memcached.config.KeyValidationType;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.metrics.YammerMetricsRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.util.Content;
import org.greencheek.caching.herdcache.util.FailingBackEndRequest;
import org.greencheek.caching.herdcache.util.ObservableBackEndRequest;
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

import static org.junit.Assert.*;

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
    private ObservableCache cache;

    @Before
    public void setUp() {

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

        cache.shutdown();

    }

    private void testHashAlgorithm(HashAlgorithm algo) {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setHashAlgorithm(algo)
                        .setKeyValidationType(KeyValidationType.NONE)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        Duration duration = Duration.ofSeconds(60);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },duration);

        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value2";
        },duration);


        assertEquals("Value should be key1", "value1", val.toBlocking().value().getValue().get());
        assertEquals("Value should be key1", "value1", val2.toBlocking().value().getValue().get());

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());
        assertEquals(1, memcached.getDaemon().getCache().getGetCmds());

    }

    @Test
    public void testJenkinsHashAlgorithm() {
        testHashAlgorithm(new JenkinsHash());
    }


    @Test
    public void testXXHashAlgorithm() {
        testHashAlgorithm(new XXHashAlogrithm());
    }

    @Test
    public void testAsciiXXHashAlgorithm() {
        testHashAlgorithm(new AsciiXXHashAlogrithm());
    }

    @Test
    public void testApplyForSameKeySetsValueOnceInMemcached() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger cacheFetchValueInvocations = new AtomicInteger(0);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cacheFetchValueInvocations.incrementAndGet();
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
            cacheFetchValueInvocations.incrementAndGet();
            return "value2";
        }, Duration.ZERO);


        assertSame(val,val2);
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
        assertEquals(1, cacheFetchValueInvocations.get());
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());
    }


    @Test
    public void testApplyForSameKeySetsValueTwiceInMemcachedAsHerdProtectionDisabled() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .disableHerdProtection()
                        .buildMemcachedConfig()
        );

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger cacheFetchValueInvocations = new AtomicInteger(0);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cacheFetchValueInvocations.incrementAndGet();
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
            cacheFetchValueInvocations.incrementAndGet();
            return "value2";
        }, Duration.ZERO);


        assertNotSame(val,val2);
        System.out.println(val);
        System.out.println(val2);

        val2.observeOn(Schedulers.immediate());
        val2.subscribe(value -> {
            System.out.println("sub2: " + Thread.currentThread().getName());
            try {
                assertEquals("Value should be key1", "value2", value.getValue().get());
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
        assertEquals(2, cacheFetchValueInvocations.get());
        assertEquals(2, memcached.getDaemon().getCache().getSetCmds());
    }

    @Test
    public void testSupplierValueIsNotCacheable() {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        Duration duration = Duration.ofSeconds(60);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },duration,(value) -> false);

        assertEquals("Value should be key1", "value1", val.toBlocking().value().getValue().get());


        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value2";
        },duration);



        assertEquals("Value should be key1", "value2", val2.toBlocking().value().getValue().get());

        // we override the same key
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        // first item is not cacheable, so we do not save it
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());
        // two gets on the cache which are misses
        assertEquals(2, memcached.getDaemon().getCache().getGetCmds());
    }

    @Test
    public void testCacheValueIsNotValid() {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        Duration duration = Duration.ofSeconds(60);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },duration,(value) -> true);

        assertEquals("Value should be key1", "value1", val.toBlocking().value().getValue().get());


        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value2";
        },duration,(value) -> true,(value)->false);


        assertEquals("Value should be key1", "value2", val2.toBlocking().value().getValue().get());

        // we override the same key
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        // first item is not cacheable, so we do not save it
        assertEquals(2, memcached.getDaemon().getCache().getSetCmds());
        // two gets on the cache which are misses
        assertEquals(2, memcached.getDaemon().getCache().getGetCmds());
    }


    @Test
    public void testNoHashingPerformedOnKey() {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        Duration duration = Duration.ofSeconds(60);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },duration,(value) -> true);


        CacheItem<String> item = val.toBlocking().value();
        assertTrue(item.getKey().startsWith("elastic"));
        assertTrue(item.getKey().endsWith("Key1"));

        // we override the same key
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        // first item is not cacheable, so we do not save it
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());
        // two gets on the cache which are misses
        assertEquals(1, memcached.getDaemon().getCache().getGetCmds());
    }

    @Test
    public void testWaitForCacheSet() {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        Duration duration = Duration.ofSeconds(60);

        AtomicInteger canSaveCacheValue = new AtomicInteger(0);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },duration,(value) -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            canSaveCacheValue.incrementAndGet();
            return true;
        },(value) -> true);


        CacheItem<String> item = val.toBlocking().value();
        assertEquals(1,canSaveCacheValue.get());

        assertTrue(item.getKey().startsWith("elastic"));
        assertTrue(item.getKey().endsWith("Key1"));

        // we override the same key
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        // first item is not cacheable, so we do not save it
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());
        // two gets on the cache which are misses
        assertEquals(1, memcached.getDaemon().getCache().getGetCmds());
    }


    @Test
    public void testNoWaitingForCacheSet() {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(false)
                        .setWaitForMemcachedSetRxScheduler(Schedulers.io())
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        Duration duration = Duration.ofSeconds(60);

        AtomicInteger canSaveCacheValue = new AtomicInteger(0);

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },duration,(value) -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            canSaveCacheValue.incrementAndGet();
            return true;
        },(value) -> true);


        CacheItem<String> item = val.toBlocking().value();
        assertEquals(0,canSaveCacheValue.get());

        assertTrue(item.getKey().startsWith("elastic"));
        assertTrue(item.getKey().endsWith("Key1"));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(1,canSaveCacheValue.get());


        // we override the same key
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        // first item is not cacheable, so we do not save it
        assertEquals(1, memcached.getDaemon().getCache().getSetCmds());
        // two gets on the cache which are misses
        assertEquals(1, memcached.getDaemon().getCache().getGetCmds());
    }

    @Test
    public void testItemExpiry() {
        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(300))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );


        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        },Duration.ofSeconds(1),(value) -> true);


        CacheItem<String> item = val.toBlocking().value();
        assertTrue(item.getKey().equals("Key1"));


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }

        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value set again";
        },Duration.ofSeconds(1),(value) -> true);


        assertEquals("value set again",val2.toBlocking().value().getValue().get());

        // we override the same key
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        // first item is not cacheable, so we do not save it
        assertEquals(2, memcached.getDaemon().getCache().getSetCmds());
        // two gets on the cache which are misses
        assertEquals(2, memcached.getDaemon().getCache().getGetCmds());
    }

    public static class Document implements Serializable {
        static final long serialVersionUID = 42L;

        private final String title;
        private final String author;
        private final String content;

        public Document(String title, String author, String content) {
            this.title = title;
            this.author = author;
            this.content = content;
        }

        public boolean equals(Object o) {
            if (o instanceof Document) {
                Document other = (Document) o;

                if (other.title.equals(this.title) &&
                        other.author.equals(this.author) &&
                        other.content.equals(this.content)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Test
    public void testSerializationInMemcachedCache() {
        cache = new SpyObservableMemcachedCache<Document>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Document nemo = new Document("Finding Nemo", "Disney", largeCacheValue);
        Document jungle = new Document("Jungle Book", "Disney", largeCacheValue);
        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return nemo;
        }, Duration.ofSeconds(100));

        assertEquals("Value should be key1", nemo, val.toBlocking().value().getValue().get());

        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return jungle;
        }, Duration.ofSeconds(100));


        assertEquals("Value should be key1", nemo, val2.toBlocking().value().getValue().get());

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

    }
//

//
    @Test
    public void testLargeCacheValue() {


        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return largeCacheValue;
        }, Duration.ZERO);

        assertEquals("Value should be key1", largeCacheValue, val.toBlocking().value().getValue().get());

        Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value2";
        }, Duration.ZERO);


        assertEquals("Value should be same as key1", largeCacheValue, val2.toBlocking().value().getValue().get());

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

    }
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

//
//
    public static class ApplicationException extends RuntimeException {

    }
//
    @Test(expected=ApplicationException.class)
    public void testApplicationExceptionIsPropagatedToCaller() throws Throwable {
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1)));
        try {
            cache = new SpyObservableMemcachedCache<>(
                    new ElastiCacheCacheConfigBuilder()
                            .setMemcachedHosts("localhost:" + memcached.getPort())
                            .setTimeToLive(Duration.ofSeconds(60))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .buildMemcachedConfig()
            );

            // Submit an item to the cache "Key1" is executing
            Single<CacheItem<String>> val = cache.apply("Key1", () -> {
                throw new ApplicationException();
            }, Duration.ZERO);

            // will throw an error
            CacheItem<String> item = val.toBlocking().value();
            fail("expected exception to be raised");

        } catch (Exception e) {
            throw e;
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void testYammerMetricsRecording() {
        MetricRegistry registry = new MetricRegistry();

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
                        .buildMemcachedConfig()
        );

        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        try {
            Single<CacheItem<String>> val = cache.apply("Key1", () -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "value1";
            }, Duration.ofSeconds(2));


            Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "value2";
            }, Duration.ofSeconds(2));


            assertEquals("Value should be key1", "value1", val.toBlocking().value().getValue().get());
            assertEquals("Value should be key1", "value1", val2.toBlocking().value().getValue().get());

            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

            Set<String> metricNames = registry.getNames();
            assertTrue(metricNames.size() > 0);

            assertTrue(metricNames.containsAll(new ArrayList() {{
                add("distributed_cache_count");
                add("distributed_cache_misscount");
                add("distributed_cache_missrate");
                add("distributed_cache_timer");
                add("distributed_cache_writes_count");
                add("value_calculation_cache_hitcount");
                add("value_calculation_cache_hitrate");
                add("value_calculation_success_count");
                add("value_calculation_time_timer");
            }}));

        } finally {
            reporter.report();
            reporter.stop();
        }
    }

//
    @Test
    public void testDoNotUseCachedSerialisedContent() {
        MetricRegistry registry = new MetricRegistry();

        Predicate<Content> cachedValueAllowed  = (Content value) ->
                System.currentTimeMillis() - value.getCreationDateEpoch() < 500;



        cache = new SpyObservableMemcachedCache<Content>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
                        .buildMemcachedConfig()
        );

        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        try {
            // this will cache the value
            Single<CacheItem<Content>>  val = cache.apply("Key1", () -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return new Content("not_cacheable");
            },Duration.ofSeconds(10), Cache.CAN_ALWAYS_CACHE_VALUE, cachedValueAllowed);


            assertEquals("Value should be key1", "not_cacheable", val.toBlocking().value().getValue().get().getContent());

            // wait for > 500 millis
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // This will not cache
            Single<CacheItem<Content>>  val2 = cache.apply("Key1", () -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return new Content("someothervalue");
            }, Duration.ofSeconds(10), (x) -> false, cachedValueAllowed);


            assertEquals("Value should be key1", "someothervalue", val2.toBlocking().value().getValue().get().getContent());

            Single<CacheItem<Content>> val3 = cache.get("Key1");
            assertEquals("Value should be key1", "not_cacheable",val3.toBlocking().value().getValue().get().getContent());


            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

            Set<String> metricNames = registry.getNames();
            assertTrue(metricNames.size() > 0);
        } finally {
            reporter.report();
            reporter.stop();
        }
    }

    @Test
    public void testResponseFromHystrixFallbackIsReturned() {

        cache = new SpyObservableMemcachedCache<Content>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        RestClient timeoutClient = new RestClient() {
            @Override
            public Content get(String key) {
                try {
                    Thread.sleep(20000);
                    return new Content("content");
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
            }

            public String toString() {
                return "timeoutClient";
            }
        };

        RestClient baseClient = new RestClient() {
            @Override
            public Content get(String key) {
                return new Content("content_from_client");
            }

            public String toString() {
                return "baseClient";
            }
        };


        assertEquals("content_from_client", new ObservableBackEndRequest("World",baseClient,cache).execute().getContent());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HystrixCommand<Content> content = new ObservableBackEndRequest("World",timeoutClient,cache);

        assertEquals("content_from_client", content.execute().getContent());

        assertTrue(content.isResponseFromFallback());
    }


    @Test
    public void testResponseFromHystrixFallbackIsNotCached() {

        cache = new SpyObservableMemcachedCache<Content>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        RestClient timeoutClient = new RestClient() {
            @Override
            public Content get(String key) {
                try {
                    Thread.sleep(20000);
                    return new Content("content");
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
            }

            public String toString() {
                return "timeoutClient";
            }
        };

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String key = "World";
        final HystrixCommand<Content> content = new FailingBackEndRequest(key,timeoutClient);

        Single<CacheItem<Content>> item = cache.apply(key,
                                                      () -> content.execute(),
                                                      Duration.ofSeconds(60),
                                                      (value) -> !content.isResponseFromFallback());

        item.observeOn(Schedulers.io());
        item.subscribeOn(Schedulers.io());

        assertSame(FailingBackEndRequest.FALLBACK, item.toBlocking().value().value());

        assertTrue(content.isResponseFromFallback());

        assertNull(((Single<CacheItem<Content>>)cache.get(key)).toBlocking().value().value());

        final HystrixCommand<Content> content2 = new FailingBackEndRequest(key,timeoutClient);


        item = cache.apply(key,
                () -> content2.execute(),
                Duration.ofSeconds(60),
                (value) -> content2.isResponseFromFallback());

        item.observeOn(Schedulers.io());
        item.subscribeOn(Schedulers.io());

        assertSame(FailingBackEndRequest.FALLBACK, item.toBlocking().value().value());

        assertTrue(content.isResponseFromFallback());

        assertNotNull(((Single<CacheItem<Content>>) cache.get(key)).toBlocking().value().value());



    }
}

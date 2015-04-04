package org.greencheek.caching.herdcache.memcached.util;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by dominictootell on 03/04/2015.
 */
public class TestCacheValues {
    public static String LARGE_CACHE_VALUE = "# Memcached For Spray #\n" +
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


    public static final byte[] LARGE_CACHE_VALUE_BYTES;
    static {
        byte[] content;
        try {
            content = LARGE_CACHE_VALUE.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            content = LARGE_CACHE_VALUE.getBytes();
        }

        LARGE_CACHE_VALUE_BYTES = content;
    }

    public static final byte[] LARGE_COMPRESSED_BYTES = org.iq80.snappy.Snappy.compress(TestCacheValues.LARGE_CACHE_VALUE_BYTES);
}

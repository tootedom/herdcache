package org.greencheek.caching.herdcache.util;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.greencheek.caching.herdcache.CacheWithExpiry;

import java.util.concurrent.Future;
import java.util.function.Predicate;

public class BackEndRequest extends HystrixCommand<Content> {

    Predicate<Content> cachedValueAllowed  = (Content value) -> value.getCreationDateEpoch() + System.currentTimeMillis() < 500;
    private final String key;
    private final RestClient client;
    private final CacheWithExpiry<Content> cache;
    private final Content FALLBACK = new Content("{}");

    public BackEndRequest(String key, RestClient client, CacheWithExpiry<Content> cache) {
        super(HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("BackEnd"))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(10)
                        .withMaxQueueSize(1000))

                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(1000)
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        .withExecutionIsolationThreadInterruptOnTimeout(true)));


        this.key = key;
        this.client = client;
        this.cache = cache;
    }

    @Override
    protected Content run() throws Exception {
        Future<Content> content = cache.apply(key,
                () -> client.get(key),
                com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService(),
                org.greencheek.caching.herdcache.Cache.CAN_ALWAYS_CACHE_VALUE, cachedValueAllowed);

        if(content == null) {
            throw new RuntimeException("failed to obtain key: " + key);
        } else {
            Content c =  content.get();
            return c;
        }
    }

    @Override
    protected Content getFallback() {

        Content content = null;
        try {
            content = cache.get(key).get();
            if(content == null) {
                return FALLBACK;
            } else {
                return content;
            }
        } catch (Exception e) {
            return FALLBACK;
        }

    }
}
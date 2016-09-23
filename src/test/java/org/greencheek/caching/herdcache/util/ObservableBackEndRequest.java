package org.greencheek.caching.herdcache.util;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.domain.CacheItem;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ObservableBackEndRequest extends HystrixCommand<Content> {

    Predicate<Content> cachedValueAllowed  = (Content value) -> System.currentTimeMillis() - value.getCreationDateEpoch() < 1000;
    private final String key;
    private final RestClient client;
    private final ObservableCache<Content> cache;
    private final Content FALLBACK = new Content("{}");

    public ObservableBackEndRequest(String key, RestClient client, ObservableCache<Content> cache) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("BackEnd"))
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
        Single<CacheItem<Content>> content = cache.apply(key,
                () -> client.get(key), Duration.ofSeconds(60),
                org.greencheek.caching.herdcache.Cache.CAN_ALWAYS_CACHE_VALUE, cachedValueAllowed);


        Content c = content.observeOn(Schedulers.immediate())
        .subscribeOn(Schedulers.immediate())
        .toBlocking().value().value();

        if(c==null) {
            throw new RuntimeException("failed to obtain key: " + key);
        } else {
            return c;
        }

    }

    @Override
    protected Content getFallback() {
        try {
            Content content = cache.get(key).toBlocking().value().value();
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
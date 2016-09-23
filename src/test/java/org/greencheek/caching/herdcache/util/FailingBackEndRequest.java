package org.greencheek.caching.herdcache.util;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.domain.CacheItem;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.function.Predicate;

public class FailingBackEndRequest extends HystrixCommand<Content> {

    Predicate<Content> cachedValueAllowed  = (Content value) -> System.currentTimeMillis() - value.getCreationDateEpoch() < 1000;
    private final String key;
    private final RestClient client;
    public static final Content FALLBACK = new Content("{}");


    public FailingBackEndRequest(String key, RestClient client) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("BackEnd"))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(10)
                        .withMaxQueueSize(1000))

                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(1000)
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        .withExecutionIsolationThreadInterruptOnTimeout(true)));


        this.key = key;
        this.client = client;
    }

    @Override
    protected Content run() throws Exception {
        return client.get(key);
    }

    @Override
    protected Content getFallback() {
       return FALLBACK;
    }
}
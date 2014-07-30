package org.greencheek.caching.herdcache;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 27/07/2014.
 */
public interface Cache<V extends Serializable> {
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService);
}

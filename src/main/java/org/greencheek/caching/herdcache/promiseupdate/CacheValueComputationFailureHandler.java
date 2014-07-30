package org.greencheek.caching.herdcache.promiseupdate;

/**
 * Created by dominictootell on 28/07/2014.
 */
public interface CacheValueComputationFailureHandler {
    public void onFailure(String key, Throwable e);
}

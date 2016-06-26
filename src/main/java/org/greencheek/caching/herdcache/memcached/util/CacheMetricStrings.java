package org.greencheek.caching.herdcache.memcached.util;

/**
 *
 */
public class CacheMetricStrings {
    public static final String CACHE_TYPE_VALUE_CALCULATION = "value_calculation_cache";
    public static final String CACHE_TYPE_VALUE_CALCULATION_ALL_TIMER = "value_calculation_time";
    public static final String CACHE_TYPE_VALUE_CALCULATION_SUCCESS_TIMER = "value_calculation_success_latency";
    public static final String CACHE_TYPE_VALUE_CALCULATION_FAILURE_TIMER = "value_calculation_failure_latency";
    public static final String CACHE_TYPE_VALUE_CALCULATION_SUCCESS_COUNTER = "value_calculation_success";
    public static final String CACHE_TYPE_VALUE_CALCULATION_FAILURE_COUNTER = "value_calculation_failure";
    public static final String CACHE_TYPE_VALUE_CALCULATION_REJECTION_COUNTER= "value_calculation_rejected_execution";
    public static final String CACHE_TYPE_STALE_VALUE_CALCULATION = "stale_value_calculation_cache";
    public static final String CACHE_TYPE_CACHE_DISABLED = "disabled_cache";
    public static final String CACHE_TYPE_CACHE_DISABLED_REJECTION = "disabled_cache";

    public static final String CACHE_TYPE_STALE_CACHE = "stale_distributed_cache";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE = "distributed_cache";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE_WRITES_COUNTER="distributed_cache_writes";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE_REJECTION = "distributed_cache_rejection";
    public static final String CACHE_TYPE_ALL = "cache";
}

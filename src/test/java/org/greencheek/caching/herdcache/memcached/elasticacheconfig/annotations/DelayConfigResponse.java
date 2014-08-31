package org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 21/07/2014.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DelayConfigResponse {
    TimeUnit delayedForTimeUnit() default TimeUnit.MILLISECONDS;
    long delayFor() default -1;
}

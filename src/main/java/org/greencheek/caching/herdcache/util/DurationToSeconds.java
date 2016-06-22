package org.greencheek.caching.herdcache.util;

import java.time.Duration;

/**
 *
 */
public class DurationToSeconds {
    public static int getSeconds(Duration duration) {
        if(duration==null || duration == Duration.ZERO) {
            return 0;
        }
        else {
            long timeToLiveSec  = duration.getSeconds();
            return (int)((timeToLiveSec >= 1l) ? timeToLiveSec : 0);
        }
    }
}

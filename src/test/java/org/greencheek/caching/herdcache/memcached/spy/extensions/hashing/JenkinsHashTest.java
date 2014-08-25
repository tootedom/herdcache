package org.greencheek.caching.herdcache.memcached.spy.extensions.hashing;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class JenkinsHashTest {

    private static final Map<String,String> properties = new HashMap<String,String>() {{
        put("sausage","2834523395");
        put("blubber","1103975961");
        put("pencil","3318404908");
        put("cloud","670342857");
        put("moon","2385442906");
        put("water","3403519606");
        put("computer","2375101981");
        put("school","1513618861");
        put("network","2981967937");
        put("hammer","1218821080");
    }};

    @Test
    public void testHash() throws Exception {
        JenkinsHash j = new JenkinsHash();
        Properties p = new Properties();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            long result = j.hash((String)entry.getKey());
            // Print out hash mismatches
            if (result != Long.parseLong((String) entry.getValue())) {
                System.out.println("Key: " + (String)entry.getKey());
                System.out.println("Expected Hash Value: " + Long.parseLong((String) entry.getValue()));
                System.out.println("Actual Hash Value: " + result);
            }
            assertEquals(result, Long.parseLong((String) entry.getValue()));
        }
    }
}
package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ElastiCacheHost;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultElastiCacheConfigParserTest {

    private ElastiCacheConfigParser parser = new DefaultElastiCacheConfigParser();
    private static String twoHostsString = "localhost|127.0.0.1|1234 localhost|127.0.0.1|3456";
    private static String twoHostsStringNoIps = "localhost||1234 localhost||3456";

    @Test
    public void testFullConfigLine() {
        assertEquals("should parse 2 hosts",2,parser.parseServers(twoHostsString).size());

        List<ElastiCacheHost> hosts = parser.parseServers(twoHostsString);

        assertEquals("First Host should have the name localhost","localhost",hosts.get(0).getHostName());
        assertEquals("First Host should have the ip 127.0.0.1","127.0.0.1",hosts.get(0).getIp());
        assertEquals("First Host should have the port 1234 ",1234,hosts.get(0).getPort());

        assertEquals("2nd Host should have the name localhost","localhost",hosts.get(1).getHostName());
        assertEquals("2nd Host should have the ip 127.0.0.1","127.0.0.1",hosts.get(1).getIp());
        assertEquals("2nd Host should have the port 3456 ",3456,hosts.get(1).getPort());
    }



    @Test
    public void testFullConfigLineNoIps() {
        assertEquals("should parse 2 hosts",2,parser.parseServers(twoHostsStringNoIps).size());

        List<ElastiCacheHost> hosts = parser.parseServers(twoHostsStringNoIps);

        assertEquals("First Host should have the name localhost","localhost",hosts.get(0).getHostName());
        assertFalse("First Host should have an empty ip", hosts.get(0).hasIP());
        assertEquals("First Host should have an empty ip","",hosts.get(0).getIp());
        assertEquals("First Host should have the port 1234 ", 1234, hosts.get(0).getPort());

        assertEquals("2nd Host should have the name localhost","localhost",hosts.get(1).getHostName());
        assertFalse("First Host should have an empty ip",hosts.get(1).hasIP());
        assertEquals("2nd Host should have an empty ip","",hosts.get(1).getIp());
        assertEquals("2nd Host should have the port 3456 ",3456,hosts.get(1).getPort());
    }

}
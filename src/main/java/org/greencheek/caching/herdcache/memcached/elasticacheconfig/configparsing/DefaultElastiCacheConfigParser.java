package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ElastiCacheHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dominictootell on 30/08/2014.
 */
public class DefaultElastiCacheConfigParser implements ElastiCacheConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(ElastiCacheConfigParser.class);

    @Override
    public List<ElastiCacheHost> parseServers(String serversString) {

        List<String> separatedHosts = SplitByChar.split(serversString,' ',true);

        List<ElastiCacheHost> elastiCacheHosts = new ArrayList<ElastiCacheHost>();
        int size = separatedHosts.size();

        int i = 0;
        while(i<size) {
            String hostString = separatedHosts.get(i);
            i+=1;

            List<String> hostInfo = SplitByChar.split(hostString,'|',false);

            if(hostInfo.size()==3) {
                String hostName = hostInfo.get(0).trim();
                String hostIP = hostInfo.get(1).trim();
                String hostPort = hostInfo.get(2).trim();

                try {
                    elastiCacheHosts.add(new ElastiCacheHost(hostName, hostIP, Integer.parseInt(hostPort), hostIP.length() > 0));
                } catch (NumberFormatException e){
                    logger.warn("Invalid port number ({}) specified for host:{}",hostName,hostPort);

                }


            }
        }

        return elastiCacheHosts;

    }
}

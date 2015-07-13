package org.greencheek.caching.herdcache.memcached.factory;

import com.spotify.folsom.MemcacheClient;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.internal.CheckedOperationTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class FolsomReferencedClient implements ReferencedClient {

    private static final Logger logger = LoggerFactory.getLogger(FolsomReferencedClient.class);
    private final boolean isAvailable;
    private final List<InetSocketAddress> resolvedHosts;
    private final MemcacheClient client;

    public FolsomReferencedClient(boolean isAvailable,
                                  List<InetSocketAddress> resolvedHosts,
                                  MemcacheClient client) {
        this.isAvailable = isAvailable;
        this.resolvedHosts = resolvedHosts;
        this.client = client;
    }


    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public List<InetSocketAddress> getResolvedHosts() {
        return resolvedHosts;
    }

    @Override
    public Object get(String key, long timeout, TimeUnit unit) {
        Object value = null;
        try {
            Future<Object> future =  client.get(key);
            value = future.get(timeout,unit);
        } catch ( OperationTimeoutException | CheckedOperationTimeoutException e) {
            logger.warn("timeout when retrieving key {} from memcached",key);
        } catch (TimeoutException e) {
            logger.warn("timeout when retrieving key {} from memcached", key);
        } catch(Exception e) {
            logger.warn("Unable to contact memcached for get({}): {}", key, e.getMessage());
        } catch(Throwable e) {
            logger.warn("Exception thrown when communicating with memcached for get({}): {}", key, e.getMessage());
        }
        return value;
    }

    @Override
    public Future set(String key, int ttlInSeconds, Object value) {

        return client.set(key, value, ttlInSeconds);
    }

    @Override
    public Future delete(String key) {
        return client.delete(key);
    }

    /**
     * Folsom does not implement flush
     * @return
     */
    @Override
    public Future flush() {
        return null;
    }

    @Override
    public void shutdown() {
        client.shutdown();
    }
}

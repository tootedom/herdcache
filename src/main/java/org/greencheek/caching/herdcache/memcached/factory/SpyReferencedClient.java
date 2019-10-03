package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.internal.CheckedOperationTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class SpyReferencedClient<V> implements ReferencedClient {

    private static final Logger logger = LoggerFactory.getLogger(SpyReferencedClient.class);
    public static final ReferencedClient UNAVAILABLE_REFERENCE_CLIENT = new SpyReferencedClient(false, Collections.<InetSocketAddress>emptyList(),null);

    private final boolean isAvailable;
    private final List<InetSocketAddress> resolvedHosts;
    private final MemcachedClientIF client;


    public SpyReferencedClient(boolean isAvailable,
                               List<InetSocketAddress> resolvedHosts,
                               MemcachedClientIF client) {
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
            Future<Object> future =  client.asyncGet(key);
            value = future.get(timeout,unit);
        } catch ( OperationTimeoutException | CheckedOperationTimeoutException e) {
            logger.warn("timeout when retrieving key {} from memcached.  Error: {}", key, e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("timeout when retrieving key {} from memcached.  Error: {}", key, e.getMessage());
        } catch(Exception e) {
            logger.warn("Unable to contact memcached for get({}).  Error: {}", key, e.getMessage(), e);
        } catch(Throwable e) {
            logger.warn("Exception thrown when communicating with memcached for get({}).  Error: {}", key, e.getMessage(),e);
        }
        return value;
    }

    @Override
    public Future set(String key, int entryTTLInSeconds, Object value) {
        return client.set(key, entryTTLInSeconds, value);
    }

    @Override
    public Future delete(String key) {
        return client.delete(key);
    }

    @Override
    public Future flush() {
        return client.flush();
    }

    @Override
    public void shutdown() {
        if(client!=null) {
            client.shutdown();
        }
    }

    // Only for testing, not to be used anywhere else.
    public MemcachedClientIF getMemcachedClient() {
        return client;
    }
}

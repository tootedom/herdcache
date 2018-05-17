package org.greencheek.caching.herdcache.memcached.factory;

/**
 * Created by dominictootell on 17/05/2018.
 */
public class StaticReferencedClientHolder implements ReferencedClientHolder {

    private final ReferencedClient client;

    public StaticReferencedClientHolder(ReferencedClient client) {
        this.client = client;
    }
    @Override
    public ReferencedClient getClient() {
        return null;
    }
}

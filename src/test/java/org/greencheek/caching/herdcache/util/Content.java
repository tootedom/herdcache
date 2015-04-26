package org.greencheek.caching.herdcache.util;

import java.io.Serializable;

public class Content implements Serializable {
    private static final long serialVersionUID = 1999L;


    private final long creationDateEpoch;
    private final String content;

    public Content(String content) {
        this.creationDateEpoch = System.currentTimeMillis();
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public long getCreationDateEpoch() {
        return creationDateEpoch;
    }
}
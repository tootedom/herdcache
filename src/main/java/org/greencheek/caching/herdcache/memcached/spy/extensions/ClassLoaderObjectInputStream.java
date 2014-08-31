package org.greencheek.caching.herdcache.memcached.spy.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Created by dominictootell on 05/06/2014.
 */
public class ClassLoaderObjectInputStream extends ObjectInputStream {

    public ClassLoaderObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected Class<?> resolveClass(ObjectStreamClass objectStreamClass)
            throws IOException, ClassNotFoundException
    {
        Class clazz = Class.forName(objectStreamClass.getName(), false, Thread.currentThread().getContextClassLoader());
        if (clazz != null) return clazz;
        else return super.resolveClass(objectStreamClass);
    }
}

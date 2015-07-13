package org.greencheek.caching.herdcache;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * After calling the Backend Service (i.e. the supplier), this function determines if
 * the returned value is cachable or not.
 */
public interface IsSupplierValueCachable<V extends Serializable> extends Predicate<V> {
    static final IsSupplierValueCachable GENERATED_VALUE_IS_ALWAYS_CACHABLE = (X) -> true;
}

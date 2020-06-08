package com.shadorc.shadbot.cache;

import java.util.Collection;
import java.util.Set;

public interface Cache<K, V> {

    void save(K key, V value);

    V find(K key);

    long count();

    V delete(K key);

    void deleteAll();

    Set<K> keys();

    Collection<V> values();

    void invalidate();

}

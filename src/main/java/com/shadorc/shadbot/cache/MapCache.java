package com.shadorc.shadbot.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapCache<K, V> implements Cache<K, V> {

    private final Map<K, V> map;

    public MapCache() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public void save(K key, V value) {
        this.map.put(key, value);
    }

    @Override
    public V find(K key) {
        return this.map.get(key);
    }

    @Override
    public long count() {
        return this.map.size();
    }

    @Override
    public Optional<V> delete(K key) {
        return Optional.ofNullable(this.map.remove(key));
    }

    @Override
    public void deleteAll() {
        this.map.clear();
    }

    @Override
    public Set<K> keys() {
        return this.map.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @Override
    public void invalidate() {
        this.map.clear();
    }
}

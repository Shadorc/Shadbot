package me.shadorc.shadbot.store;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.store.api.Store;

public class ShardStoreRegistry {

	private final Map<Class<?>, Store<?, ?>> valueStore = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Class<?>, Set<?>>> keyStores = new ConcurrentHashMap<>();

	boolean containsStore(Class<?> valueClass) {
		return this.valueStore.containsKey(valueClass);
	}

	<V extends Serializable, K extends Comparable<K>> void putStore(Class<V> valueClass, Store<K, V> store) {
		this.valueStore.put(valueClass, store);
	}

	@SuppressWarnings("unchecked")
	<K extends Comparable<K>, V extends Serializable> Store<K, V> getValueStore(Class<K> key, Class<V> value) {
		return (Store<K, V>) this.valueStore.get(value);
	}

	@SuppressWarnings("unchecked")
	<K extends Comparable<K>, V extends Serializable> Set<K> getKeyStore(Class<V> valueClass, int shardId) {
		return (Set<K>) this.keyStores.computeIfAbsent(shardId, k -> new ConcurrentHashMap<>())
				.computeIfAbsent(valueClass, k -> ConcurrentHashMap.newKeySet());
	}
}

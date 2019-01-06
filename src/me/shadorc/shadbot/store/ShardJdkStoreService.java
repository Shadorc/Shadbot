package me.shadorc.shadbot.store;

import java.io.Serializable;

import discord4j.store.api.Store;
import discord4j.store.api.util.StoreContext;
import discord4j.store.jdk.JdkStore;
import discord4j.store.jdk.JdkStoreService;
import reactor.core.publisher.Mono;

public class ShardJdkStoreService extends JdkStoreService {

	private final ShardStoreRegistry registry;

	volatile Class<?> messageClass;
	volatile int shardId;

	public ShardJdkStoreService(ShardStoreRegistry registry) {
		this.registry = registry;
	}

	@Override
	public <K extends Comparable<K>, V extends Serializable> Store<K, V> provideGenericStore(Class<K> keyClass,
			Class<V> valueClass) {
		if(!this.registry.containsStore(valueClass)) {
			this.registry.putStore(valueClass, new JdkStore<K, V>(!valueClass.equals(this.messageClass)));
		}
		return new ShardAwareStore<>(this.registry.getValueStore(keyClass, valueClass), this.registry.getKeyStore(valueClass, this.shardId));
	}

	@Override
	public Mono<Void> init(StoreContext context) {
		this.messageClass = context.getMessageClass();
		this.shardId = context.getShard();
		return Mono.empty();
	}
}

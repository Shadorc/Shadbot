package me.shadorc.shadbot.store;

import java.io.Serializable;
import java.util.Set;

import org.reactivestreams.Publisher;

import discord4j.store.api.Store;
import discord4j.store.api.util.WithinRangePredicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public class ShardAwareStore<K extends Comparable<K>, V extends Serializable> implements Store<K, V> {

	private final Store<K, V> valueStore;
	private final Set<K> keySet;

	public ShardAwareStore(Store<K, V> valueStore, Set<K> keySet) {
		this.valueStore = valueStore;
		this.keySet = keySet;
	}

	@Override
	public Mono<Void> save(K key, V value) {
		return this.valueStore.save(key, value)
				.then(Mono.fromRunnable(() -> this.keySet.add(key)));
	}

	@Override
	public Mono<Void> save(Publisher<Tuple2<K, V>> entryStream) {
		return Flux.from(entryStream)
				.doOnNext(t -> this.valueStore.save(t.getT1(), t.getT2()))
				.doOnNext(t -> this.keySet.add(t.getT1()))
				.then();
	}

	@Override
	public Mono<V> find(K id) {
		return this.valueStore.find(id);
	}

	@Override
	public Flux<V> findInRange(K start, K end) {
		return this.valueStore.findInRange(start, end);
	}

	@Override
	public Mono<Long> count() {
		return this.valueStore.count();
	}

	@Override
	public Mono<Void> delete(K id) {
		return this.valueStore.delete(id)
				.then(Mono.fromRunnable(() -> this.keySet.remove(id)));
	}

	@Override
	public Mono<Void> delete(Publisher<K> ids) {
		return Flux.from(ids)
				.doOnNext(this.valueStore::delete)
				.doOnNext(this.keySet::remove)
				.then();
	}

	@Override
	public Mono<Void> deleteInRange(K start, K end) {
		return this.valueStore.keys().filter(new WithinRangePredicate<>(start, end))
				.doOnNext(this.valueStore::delete)
				.doOnNext(this.keySet::remove)
				.then();
	}

	@Override
	public Mono<Void> deleteAll() {
		return this.valueStore.deleteAll()
				.then(Mono.fromRunnable(this.keySet::clear));
	}

	@Override
	public Flux<K> keys() {
		return this.valueStore.keys();
	}

	@Override
	public Flux<V> values() {
		return this.valueStore.values();
	}

	@Override
	public Mono<Void> invalidate() {
		return this.delete(Flux.fromIterable(this.keySet))
				.then(Mono.fromRunnable(this.keySet::clear));
	}
}

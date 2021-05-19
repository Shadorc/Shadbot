package com.locibot.locibot.core.cache;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultiValueCache<K, V> extends ConcurrentHashMap<K, SingleValueCache<V>> {

    private final Function<? super V, Duration> ttlForValue;
    private final Function<Throwable, Duration> ttlForError;
    private final Supplier<Duration> ttlForEmpty;

    private MultiValueCache(Function<? super V, Duration> ttlForValue,
                            Function<Throwable, Duration> ttlForError,
                            Supplier<Duration> ttlForEmpty) {
        this.ttlForValue = ttlForValue;
        this.ttlForError = ttlForError;
        this.ttlForEmpty = ttlForEmpty;
    }

    public SingleValueCache<V> getOrCache(@NotNull K key, @NotNull Mono<V> value) {
        return super.computeIfAbsent(key, __ -> SingleValueCache.Builder.create(value)
                .withTtlForValue(this.ttlForValue)
                .withTtlForError(this.ttlForError)
                .withTtlForEmpty(this.ttlForEmpty)
                .build());
    }

    public static class Builder<K, V> {

        private Function<? super V, Duration> ttlForValue = __ -> Duration.ZERO;
        private Function<Throwable, Duration> ttlForError = __ -> Duration.ZERO;
        private Supplier<Duration> ttlForEmpty = () -> Duration.ZERO;

        public static <K, V> MultiValueCache.Builder<K, V> create() {
            return new Builder<>();
        }

        public Builder<K, V> withInfiniteTtl() {
            this.ttlForValue = __ -> Duration.ofMillis(Long.MAX_VALUE);
            return this;
        }

        public Builder<K, V> withTtl(Duration ttl) {
            this.ttlForValue = __ -> ttl;
            return this;
        }

        public Builder<K, V> withTtlForValue(Function<? super V, Duration> ttlForValue) {
            this.ttlForValue = ttlForValue;
            return this;
        }

        public Builder<K, V> withTtlForError(Function<Throwable, Duration> ttlForError) {
            this.ttlForError = ttlForError;
            return this;
        }

        public Builder<K, V> withTtlForEmpty(Supplier<Duration> ttlForEmpty) {
            this.ttlForEmpty = ttlForEmpty;
            return this;
        }

        public MultiValueCache<K, V> build() {
            return new MultiValueCache<>(this.ttlForValue, this.ttlForError, this.ttlForEmpty);
        }

    }

}

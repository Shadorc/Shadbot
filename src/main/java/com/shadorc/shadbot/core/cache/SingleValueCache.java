package com.shadorc.shadbot.core.cache;

import org.jetbrains.annotations.NotNull;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class SingleValueCache<T> extends Mono<T> {

    private final Mono<T> cachedValue;

    private SingleValueCache(Mono<T> cachedValue) {
        this.cachedValue = cachedValue;
    }

    @Override
    public void subscribe(@NotNull CoreSubscriber<? super T> actual) {
        this.cachedValue.subscribe(actual);
    }

    public static class Builder<T> {

        private final Mono<T> value;

        private Function<? super T, Duration> ttlForValue = __ -> Duration.ZERO;
        private Function<Throwable, Duration> ttlForError = __ -> Duration.ZERO;
        private Supplier<Duration> ttlForEmpty = () -> Duration.ZERO;

        private Builder(Mono<T> value) {
            this.value = value;
        }

        public static <T> Builder<T> create(Mono<T> value) {
            return new Builder<>(value);
        }

        public Builder<T> withTtl(Duration ttl) {
            this.ttlForValue = __ -> ttl;
            return this;
        }

        public Builder<T> withTtlForValue(Function<? super T, Duration> ttlForValue) {
            this.ttlForValue = ttlForValue;
            return this;
        }

        public Builder<T> withTtlForError(Function<Throwable, Duration> ttlForError) {
            this.ttlForError = ttlForError;
            return this;
        }

        public Builder<T> withTtlForEmpty(Supplier<Duration> ttlForEmpty) {
            this.ttlForEmpty = ttlForEmpty;
            return this;
        }

        public SingleValueCache<T> build() {
            return new SingleValueCache<>(this.value.cache(this.ttlForValue, this.ttlForError, this.ttlForEmpty));
        }

    }

}

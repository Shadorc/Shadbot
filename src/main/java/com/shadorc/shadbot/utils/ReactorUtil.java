package com.shadorc.shadbot.utils;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Predicate;

public class ReactorUtil {

    public static <T> Function<T, Mono<Boolean>> filterWhenSwitchIfFalse(Function<? super T, ? extends Publisher<Boolean>> asyncPredicate,
                                                                         Mono<?> switchMono) {
        return value -> Mono.from(asyncPredicate.apply(value))
                .flatMap(bool -> {
                    if (!bool) {
                        return switchMono
                                .thenReturn(false);
                    }
                    return Mono.just(true);
                });
    }

    public static <T> Function<T, Mono<Boolean>> filterSwitchIfFalse(Predicate<? super T> tester,
                                                                     Mono<?> switchMono) {
        return filterWhenSwitchIfFalse(
                value -> Mono.just(tester.test(value)),
                switchMono);
    }

}

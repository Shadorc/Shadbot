package com.locibot.locibot.utils;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ReactorUtilTest {

    @Test
    public void testFilterOrExecute() {
        final AtomicBoolean executed1 = new AtomicBoolean(false);
        final Integer actual1 = Mono.just(0)
                .filterWhen(ReactorUtil.filterOrExecute(
                        test -> test != 0,
                        Mono.fromRunnable(() -> executed1.set(true))))
                .block();
        assertNull(actual1);
        assertTrue(executed1.get());

        final AtomicBoolean executed2 = new AtomicBoolean(false);
        final Integer actual2 = Mono.just(0)
                .filterWhen(ReactorUtil.filterOrExecute(
                        test -> test == 0,
                        Mono.fromRunnable(() -> executed2.set(true))))
                .block();
        assertEquals(0, actual2);
        assertFalse(executed2.get());
    }

    @Test
    public void testFilterWhenOrExecute() {
        final AtomicBoolean executed1 = new AtomicBoolean(false);
        final Integer actual1 = Mono.just(0)
                .filterWhen(ReactorUtil.filterWhenOrExecute(
                        test -> Mono.just(test != 0),
                        Mono.fromRunnable(() -> executed1.set(true))))
                .block();
        assertNull(actual1);
        assertTrue(executed1.get());

        final AtomicBoolean executed2 = new AtomicBoolean(false);
        final Integer actual2 = Mono.just(0)
                .filterWhen(ReactorUtil.filterWhenOrExecute(
                        test -> Mono.just(test == 0),
                        Mono.fromRunnable(() -> executed2.set(true))))
                .block();
        assertEquals(0, actual2);
        assertFalse(executed2.get());
    }

}

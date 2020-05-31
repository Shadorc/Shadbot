package com.shadorc.shadbot.utils;

import reactor.util.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandUtils {

    /**
     * @param list The list from which to take a random element.
     * @return A random element from the list or {@code null} if the list is empty.
     */
    @Nullable
    public static <T> T randValue(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * @param array The array from which to take a random element.
     * @return A random element from the array or {@code null} if the array is empty.
     */
    @Nullable
    public static <T> T randValue(T[] array) {
        return RandUtils.randValue(Arrays.asList(array));
    }

}

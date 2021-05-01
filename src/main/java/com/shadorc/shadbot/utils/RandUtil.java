package com.shadorc.shadbot.utils;

import reactor.util.annotation.Nullable;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class RandUtil {

    /**
     * @param array The array from which to take a random element.
     * @return A random element from the array or {@code null} if the array is empty.
     */
    @Nullable
    public static <T> T randValue(T[] array) {
        if (array.length == 0) {
            return null;
        }
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    /**
     * @param collection The collection from which to take a random element.
     * @return A random element from the collection or {@code null} if the collection is empty.
     */
    @Nullable
    public static <T> T randValue(Collection<T> collection) {
        //noinspection unchecked
        return RandUtil.randValue((T[]) collection.toArray());
    }

}

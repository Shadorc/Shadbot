package com.shadorc.shadbot.utils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtils {

    /**
     * @param map The map to inverse.
     * @return The {@code map} inverted, keys become values and values become keys.
     */
    public static <K, V> Map<V, K> inverse(Map<K, V> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    /**
     * @param map        The map to sort.
     * @param comparator The {@link Comparator} to be used to compare stream elements.
     * @return A {@link LinkedHashMap} containing the elements of the {@code map} sorted using {@code comparator}.
     */
    public static <K, V> Map<K, V> sort(Map<K, V> map, Comparator<? super Map.Entry<K, V>> comparator) {
        return map.entrySet()
                .stream()
                .sorted(comparator)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (value1, value2) -> value1,
                        LinkedHashMap::new));
    }

}

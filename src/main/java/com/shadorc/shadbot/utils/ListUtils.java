package com.shadorc.shadbot.utils;

import java.util.AbstractList;
import java.util.List;

public class ListUtils {

    private static class Partition<T> extends AbstractList<List<T>> {

        private final List<T> list;
        private final int size;

        private Partition(List<T> list, int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be positive");
            }
            this.list = list;
            this.size = size;
        }

        public List<T> get(int index) {
            int start = index * this.size;
            int end = Math.min(start + this.size, this.list.size());
            return this.list.subList(start, end);
        }

        public int size() {
            return (int) Math.ceil((double) this.list.size() / this.size);
        }

        public boolean isEmpty() {
            return this.list.isEmpty();
        }
    }

    public static <T> List<List<T>> partition(List<T> list, int size) {
        return new Partition<>(list, size);
    }
}

package me.shadorc.shadbot.data.stats.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleStat <T extends Enum<T>> {

	private final Map<T, AtomicLong> map;

	public SimpleStat() {
		this.map = new HashMap<>();
	}

	public void log(T key) {
		map.computeIfAbsent(key, ignored -> new AtomicLong(0)).incrementAndGet();
	}

}

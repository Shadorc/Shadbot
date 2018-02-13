package me.shadorc.shadbot.data.stats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;

public class StatsObject {

	private final String name;
	private final ConcurrentHashMap<String, AtomicLong> map;

	public StatsObject(String name, JSONObject obj) {
		this.name = name;
		this.map = new ConcurrentHashMap<>();

		if(obj != null) {
			for(String key : obj.keySet()) {
				map.put(key, new AtomicLong(Long.parseLong(obj.get(key).toString())));
			}
		}
	}

	public void increment(String stat, int count) {
		map.computeIfAbsent(stat, key -> new AtomicLong(0)).addAndGet(count);
	}

	public String getName() {
		return name;
	}

	public ConcurrentHashMap<String, AtomicLong> getMap() {
		return map;
	}

}

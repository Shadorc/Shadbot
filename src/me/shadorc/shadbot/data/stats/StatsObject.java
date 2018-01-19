package me.shadorc.shadbot.data.stats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

public class StatsObject {

	private final String name;
	private final ConcurrentHashMap<String, AtomicInteger> map;

	public StatsObject(String name, JSONObject obj) {
		this.name = name;
		this.map = new ConcurrentHashMap<>();

		if(obj != null) {
			for(String key : obj.keySet()) {
				map.put(key, new AtomicInteger(Integer.parseInt(obj.get(key).toString())));
			}
		}
	}

	public void increment(String stat, int count) {
		map.computeIfAbsent(stat, key -> new AtomicInteger(0)).addAndGet(count);
	}

	public String getName() {
		return name;
	}

	public ConcurrentHashMap<String, AtomicInteger> getMap() {
		return map;
	}

}

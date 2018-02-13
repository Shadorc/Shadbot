package me.shadorc.shadbot.data.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.annotation.StatsEnum;
import me.shadorc.shadbot.data.lotto.LottoManager;
import me.shadorc.shadbot.data.stats.Stats.MoneyEnum;

public class StatsManager {

	private static final String FILE_NAME = "stats.json";
	private static final File FILE = new File(FILE_NAME);

	// <StatsEnum, StatsObject>
	private static final ConcurrentHashMap<String, StatsObject> STATS_MAP = new ConcurrentHashMap<>();

	@DataInit
	public static void init() throws IOException {
		if(!FILE.exists()) {
			try (FileWriter writer = new FileWriter(FILE)) {
				writer.write(new JSONObject().toString(Config.JSON_INDENT_FACTOR));
			}
		}

		JSONObject dataObj;
		try (InputStream stream = FILE.toURI().toURL().openStream()) {
			dataObj = new JSONObject(new JSONTokener(stream));
		}

		/*
		 * Brace yourself, explanations are coming
		 * ...
		 * No, I have no idea how it works, good luck
		 * 
		 * Load all enumerations presents in Stats.class and store them in the map
		 * If the statistic is subdivided, example MONEY_GAINS has one value per command, we create a new StatsObject for every enum
		 */
		Reflections reflections = new Reflections(Stats.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> statsClass : reflections.getTypesAnnotatedWith(StatsEnum.class)) {
			StatsEnum statsEnum = statsClass.getAnnotation(StatsEnum.class);
			if(statsEnum.isSubdivided()) {
				for(Object obj : statsClass.getEnumConstants()) {
					String name = obj.toString().toLowerCase();
					STATS_MAP.put(name, new StatsObject(name, dataObj.optJSONObject(name)));
				}
			} else {
				String name = statsEnum.name();
				STATS_MAP.put(statsClass.getSimpleName().toLowerCase(), new StatsObject(name, dataObj.optJSONObject(name)));
			}
		}
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 10, period = 10, unit = TimeUnit.MINUTES)
	public static void save() throws JSONException, IOException {
		JSONObject mainObj = new JSONObject();
		for(String key : STATS_MAP.keySet()) {
			mainObj.put(STATS_MAP.get(key).getName(), new JSONObject(STATS_MAP.get(key).getMap()));
		}

		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(mainObj.toString(Config.JSON_INDENT_FACTOR));
		}
	}

	/**
	 * @param stat - an enumeration contained in Stats.class
	 * @param key - a specific key for subdivided statistics
	 * @param count - the count to increment
	 */
	public static void increment(Object stat, String key, int count) {
		if(MoneyEnum.MONEY_LOST.equals(stat)) {
			LottoManager.addToPool(count);
		}
		STATS_MAP.get(stat.toString().toLowerCase()).increment(key, count);
	}

	public static void increment(Object stat, String key) {
		StatsManager.increment(stat, key, 1);
	}

	public static void increment(Object stat, int count) {
		STATS_MAP.get(stat.getClass().getSimpleName().toLowerCase()).increment(stat.toString().toLowerCase(), count);
	}

	public static void increment(Object stat) {
		StatsManager.increment(stat, 1);
	}

	public static Map<String, Long> get(String key) {
		if(!STATS_MAP.containsKey(key.toLowerCase())) {
			return null;
		}

		Map<String, Long> map = new HashMap<>();
		ConcurrentHashMap<String, AtomicLong> concurrentMap = STATS_MAP.get(key.toLowerCase()).getMap();
		concurrentMap.keySet().stream().forEach(concurrentKey -> map.put(concurrentKey, concurrentMap.get(concurrentKey).get()));

		return map;
	}

	public static List<String> getKeys() {
		return new ArrayList<>(STATS_MAP.keySet());
	}

}
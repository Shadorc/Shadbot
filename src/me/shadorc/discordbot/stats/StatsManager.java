package me.shadorc.discordbot.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.utils.LogUtils;

public class StatsManager {

	private static final File STATS_FILE = new File("stats.json");
	private static final ConcurrentHashMap<StatsEnum, Map<String, AtomicLong>> STATS_MAP = new ConcurrentHashMap<>();

	static {
		if(!STATS_FILE.exists()) {
			try (FileWriter writer = new FileWriter(STATS_FILE)) {
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occurred during stats file creation. Exiting.", err);
				System.exit(1);
			}
		}

		try (InputStream stream = STATS_FILE.toURI().toURL().openStream()) {
			JSONObject statsObj = new JSONObject(new JSONTokener(stream));
			for(Object key : statsObj.keySet()) {
				Map<String, AtomicLong> map = new HashMap<String, AtomicLong>();
				JSONObject subStatsObj = statsObj.getJSONObject(key.toString());
				for(Object subKey : subStatsObj.keySet()) {
					map.put(subKey.toString(), new AtomicLong(subStatsObj.getLong(subKey.toString())));
				}
				STATS_MAP.put(StatsEnum.valueOf(key.toString().toUpperCase()), map);
			}
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occurred during stats file initialisation. Exiting.", err);
			System.exit(1);
		}
	}

	private static void increment(StatsEnum statsEnum, String key, int count) {
		STATS_MAP.putIfAbsent(statsEnum, new HashMap<>());
		STATS_MAP.get(statsEnum).putIfAbsent(key, new AtomicLong(0));
		STATS_MAP.get(statsEnum).get(key).addAndGet(count);
	}

	public static void increment(String gameName, int coins) {
		StatsEnum statsEnum;
		if(coins < 0) {
			LottoDataManager.addToPool(Math.abs(coins));
			statsEnum = StatsEnum.MONEY_LOSSES_COMMAND;
		} else {
			statsEnum = StatsEnum.MONEY_GAINS_COMMAND;
		}

		StatsManager.increment(statsEnum, gameName, Math.abs(coins));
	}

	public static void increment(StatsEnum statsEnum, String cmdName) {
		StatsManager.increment(statsEnum, cmdName, 1);
	}

	public static void increment(StatsEnum statsEnum) {
		StatsManager.increment(StatsEnum.VARIOUS, statsEnum.toString(), 1);
	}

	public static Map<String, AtomicLong> get(StatsEnum statsEnum) {
		return STATS_MAP.getOrDefault(statsEnum, new HashMap<>());
	}

	public static void save() {
		LogUtils.info("Saving stats...");
		JSONObject mainObj = new JSONObject();
		STATS_MAP.keySet().stream().forEach(statsEnum -> mainObj.put(statsEnum.toString(), new JSONObject(STATS_MAP.get(statsEnum))));

		try (FileWriter writer = new FileWriter(STATS_FILE)) {
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving stats.", err);
		}
		LogUtils.info("Stats saved.");
	}
}
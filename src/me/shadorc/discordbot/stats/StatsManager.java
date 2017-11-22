package me.shadorc.discordbot.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.utils.LogUtils;

public class StatsManager {

	private static final File STATS_FILE = new File("stats.json");
	private static final Map<StatsEnum, Map<String, AtomicLong>> STATS_MAP = new HashMap<>();

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
	}

	public static void increment(String gameName, int coins) {
		StatsEnum statsEnum;
		if(coins < 0) {
			LottoDataManager.addToPool(Math.abs(coins));
			statsEnum = StatsEnum.MONEY_LOST;
		} else {
			statsEnum = StatsEnum.MONEY_GAINED;
		}
		STATS_MAP.putIfAbsent(statsEnum, new HashMap<>());
		STATS_MAP.get(statsEnum).putIfAbsent(gameName, new AtomicLong(0));
		STATS_MAP.get(statsEnum).get(gameName).addAndGet(Math.abs(coins));
	}

	public static void increment(StatsEnum stats, String cmdName) {
		STATS_MAP.putIfAbsent(stats, new HashMap<>());
		STATS_MAP.get(stats).putIfAbsent(cmdName, new AtomicLong(0));
		STATS_MAP.get(stats).get(cmdName).incrementAndGet();
	}

	public static void increment(StatsEnum stats) {
		STATS_MAP.putIfAbsent(StatsEnum.VARIOUS, new HashMap<>());
		STATS_MAP.get(StatsEnum.VARIOUS).putIfAbsent(stats.toString(), new AtomicLong(0));
		STATS_MAP.get(StatsEnum.VARIOUS).get(stats.toString()).incrementAndGet();
	}

	public static Map<String, AtomicLong> get(StatsEnum stats) {
		return STATS_MAP.getOrDefault(stats, new HashMap<>());
	}

	public static void save() {
		JSONObject mainObj = new JSONObject();
		STATS_MAP.keySet().stream().forEach(statsEnum -> mainObj.put(statsEnum.toString(), new JSONObject(STATS_MAP.get(statsEnum))));

		try (FileWriter writer = new FileWriter(STATS_FILE)) {
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving stats.", err);
		}
	}
}
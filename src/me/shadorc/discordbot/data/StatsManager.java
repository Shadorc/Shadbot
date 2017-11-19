package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;

public class StatsManager {

	private static final File STATS_FILE = new File("stats.json");
	private static final ConcurrentHashMap<StatCategory, JSONObject> STATS_MAP = new ConcurrentHashMap<>();

	static {
		if(!STATS_FILE.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(STATS_FILE);
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occurred during stats file creation. Exiting.", err);
				System.exit(1);

			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(STATS_FILE.toURI().toURL().openStream()));
			for(StatCategory cat : StatCategory.values()) {
				STATS_MAP.put(cat, mainObj.has(cat.toString()) ? mainObj.getJSONObject(cat.toString()) : new JSONObject());
			}

		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occurred during stats file initialization. Exiting.", err);
			System.exit(1);
		}
	}

	public static JSONObject getCategory(StatCategory category) {
		return STATS_MAP.get(category);
	}

	public static void increment(StatCategory category, String key) {
		STATS_MAP.put(category, STATS_MAP.get(category).increment(key));
	}

	public static void updateGameStats(String key, int coins) {
		StatCategory category = coins > 0 ? StatCategory.MONEY_GAINS_COMMAND : StatCategory.MONEY_LOSSES_COMMAND;
		STATS_MAP.put(category, STATS_MAP.get(category).put(key, STATS_MAP.get(category).optInt(key) + Math.abs(coins)));
	}

	public static void save() {
		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject();
			for(StatCategory cat : StatCategory.values()) {
				mainObj.put(cat.toString(), STATS_MAP.get(cat));
			}

			writer = new FileWriter(STATS_FILE);
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving stats.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}

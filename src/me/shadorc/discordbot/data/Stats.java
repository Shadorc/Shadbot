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

public class Stats {

	private static final File STATS_FILE = new File("stats.json");
	private static final ConcurrentHashMap<Category, JSONObject> STATS_MAP = new ConcurrentHashMap<>();

	static {
		if(!STATS_FILE.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(STATS_FILE);
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occured during stats file creation. Exiting.", err);
				System.exit(1);

			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(STATS_FILE.toURI().toURL().openStream()));
			for(Category cat : Category.values()) {
				STATS_MAP.put(cat, mainObj.has(cat.toString()) ? mainObj.getJSONObject(cat.toString()) : new JSONObject());
			}

		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occured during stats file initialization. Exiting.", err);
			System.exit(1);
		}
	}

	public enum Category {
		UNKNOWN_COMMAND("unknown_command"),
		LIMITED_COMMAND("limited_command"),
		MONEY_GAINS_COMMAND("money_gains_command"),
		MONEY_LOSSES_COMMAND("money_losses_command"),
		HELP_COMMAND("help_command"),
		COMMAND("command");

		private final String key;

		Category(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return key;
		}
	}

	public static void increment(Category category, String key) {
		STATS_MAP.put(category, STATS_MAP.get(category).increment(key));
	}

	public static void increment(Category category, String key, int value) {
		STATS_MAP.put(category, STATS_MAP.get(category).put(key, STATS_MAP.get(category).optInt(key) + value));
	}

	public static void save() {
		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject();
			for(Category cat : Category.values()) {
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

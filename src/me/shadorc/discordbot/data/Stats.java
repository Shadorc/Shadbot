package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;

public class Stats {

	private static final ConcurrentHashMap<Category, JSONObject> STATS_MAP = new ConcurrentHashMap<>();
	private static final File STATS_FILE = new File("stats.json");

	public enum Category {
		UNKNOWN_COMMAND("unknown_command"),
		LIMITED_COMMAND("limited_command"),
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

	public static void init() {
		try {
			if(STATS_FILE.exists()) {
				JSONObject mainObj = new JSONObject(new JSONTokener(STATS_FILE.toURI().toURL().openStream()));
				for(Category cat : Category.values()) {
					STATS_MAP.put(cat, mainObj.has(cat.toString()) ? mainObj.getJSONObject(cat.toString()) : new JSONObject());
				}

			} else {
				FileWriter writer = null;
				try {
					STATS_FILE.createNewFile();
					writer = new FileWriter(STATS_FILE);
					writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
					writer.flush();
				} finally {
					IOUtils.closeQuietly(writer);
				}
			}

		} catch (IOException err) {
			LogUtils.error("An error occured during stats file initialization.", err);
		}
	}

	public static void increment(Category category, String key) {
		STATS_MAP.put(category, STATS_MAP.get(category).increment(key));
	}

	public static void save() {
		if(!STATS_FILE.exists()) {
			Stats.init();
		}

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

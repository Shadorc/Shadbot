package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;

public class Stats {

	private static final File STATS_FILE = new File("stats.json");
	private static final int INDENT_FACTOR = 2;

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

	private static void init() {
		FileWriter writer = null;
		try {
			STATS_FILE.createNewFile();
			writer = new FileWriter(STATS_FILE);
			writer.write(new JSONObject().toString(INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("An error occured during stats file initialization.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public static synchronized void increment(Category category, String key) {
		if(!STATS_FILE.exists()) {
			Stats.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(STATS_FILE.toURI().toURL().openStream()));
			JSONObject categObj = mainObj.has(category.toString()) ? mainObj.getJSONObject(category.toString()) : new JSONObject();
			categObj.increment(key);
			mainObj.put(category.toString(), categObj);

			writer = new FileWriter(STATS_FILE);
			writer.write(mainObj.toString(INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while incrementing stat.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}

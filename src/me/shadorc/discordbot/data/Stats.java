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

	public static synchronized void addUnknownCommand(String command) {
		if(!STATS_FILE.exists()) {
			Stats.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(STATS_FILE.toURI().toURL().openStream()));
			mainObj.append("unknown_commands", command);

			writer = new FileWriter(STATS_FILE);
			writer.write(mainObj.toString(INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving unknown command.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public static synchronized void increment(String key) {
		if(!STATS_FILE.exists()) {
			Stats.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(STATS_FILE.toURI().toURL().openStream()));
			mainObj.increment(key);

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

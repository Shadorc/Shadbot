package me.shadorc.discordbot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.utility.Log;

public class Storage {

	private static final File API_KEYS_FILE = new File("api_keys.json");
	private static final File DATA_FILE = new File("data.json");

	public enum API_KEYS {
		GIPHY_API_KEY,
		DTC_API_KEY,
		DISCORD_TOKEN,
		TWITTER_API_KEY,
		TWITTER_API_SECRET,
		TWITTER_TOKEN,
		TWITTER_TOKEN_SECRET,
		CLEVERBOT_API_KEY
	}

	private static void init() {
		if(!DATA_FILE.exists()) {
			try {
				DATA_FILE.createNewFile();
				FileWriter writer = null;
				try {
					writer = new FileWriter(DATA_FILE);
					writer.write("{}");
					writer.flush();
				} catch (IOException e) {
					Log.error("Error while saving in storage file.", e);
				} finally {
					try {
						if(writer != null) {
							writer.close();
						}
					} catch (IOException e) {
						Log.error("Error while closing writer.", e);
					}
				}
			} catch (IOException e) {
				Log.error("Error while creating storage file.", e);
			}
		}
	}

	public static void store(Object key, Object value) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));
			obj.put(key.toString(), value.toString());

			writer = new FileWriter(DATA_FILE);
			writer.write(obj.toString(2));
			writer.flush();
		} catch (IOException e) {
			Log.error("Error while saving in storage file.", e);
		} finally {
			try {
				if(writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				Log.error("Error while closing writer.", e);
			}
		}
	}

	public static int get(String key) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));
			if(obj.has(key)) {
				return obj.getInt(key);
			}
		} catch (JSONException | IOException e) {
			Log.error("Error while reading storage file.", e);
		}
		return 0;
	}

	public static String get(API_KEYS key) {
		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(API_KEYS_FILE.getPath())), StandardCharsets.UTF_8));
			return obj.getString(key.toString());
		} catch (JSONException | IOException e) {
			Log.error("Error while accessing to API keys storage.", e);
		}
		return null;
	}
}
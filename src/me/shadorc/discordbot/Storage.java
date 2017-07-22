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
import sx.blah.discord.handle.obj.IGuild;

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
					writer.write(new JSONObject().toString());
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

	public static void store(IGuild guild, Long key, Object value) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));

			if(!mainObj.has(guild.getStringID())) {
				mainObj.put(guild.getStringID(), new JSONObject());
			}
			mainObj.getJSONObject(guild.getStringID()).put(key.toString(), value.toString());

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(2));
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

	public static String get(IGuild guild, Long key) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));
			if(mainObj.has(guild.getStringID())) {
				return mainObj.getJSONObject(guild.getStringID()).getString(key.toString());
			}
		} catch (JSONException | IOException e) {
			Log.error("Error while reading storage file.", e);
		}
		return null;
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
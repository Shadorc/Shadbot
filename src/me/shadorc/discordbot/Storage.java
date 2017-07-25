package me.shadorc.discordbot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.handle.obj.IGuild;

public class Storage {

	private static final File API_KEYS_FILE = new File("api_keys.json");
	private static final File DATA_FILE = new File("data.json");

	public enum ApiKeys {
		GIPHY_API_KEY,
		DTC_API_KEY,
		DISCORD_TOKEN,
		TWITTER_API_KEY,
		TWITTER_API_SECRET,
		TWITTER_TOKEN,
		TWITTER_TOKEN_SECRET
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

	public static synchronized void store(IGuild guild, Object key, Object value) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));

			//If guild has never been saved before, create new guild JSONObject
			if(!mainObj.has(guild.getStringID())) {
				mainObj.put(guild.getStringID(), new JSONObject());
			}
			JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
			if(key instanceof Long) {
				guildObj.put(key.toString(), value.toString());
			} else if(key instanceof String) {
				if(guildObj.has(key.toString())) {
					guildObj.put("allowedChannels", ((JSONArray) Storage.get(guild, "allowedChannels")).put(value.toString()));
				} else {
					guildObj.put("allowedChannels", new JSONArray().put(value.toString()));
				}
			}

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

	public static synchronized Object get(IGuild guild, Object key) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));
			if(mainObj.has(guild.getStringID())) {
				JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
				if(key instanceof Long) {
					if(guildObj.has(key.toString())) {
						return guildObj.getString(key.toString());
					} else {
						return "0";
					}
				} else if(key instanceof String) {
					if(guildObj.has(key.toString())) {
						return guildObj.getJSONArray(key.toString());
					}
				}
			}
		} catch (IOException e) {
			Log.error("Error while reading data file.", e);
		}
		return null;
	}

	public static synchronized String get(ApiKeys key) {
		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(API_KEYS_FILE.getPath())), StandardCharsets.UTF_8));
			return obj.getString(key.toString());
		} catch (IOException e) {
			Log.error("Error while accessing to API keys file.", e);
		}
		return null;
	}
}
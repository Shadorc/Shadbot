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
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File API_KEYS_FILE = new File("api_keys.json");
	private static final File DATA_FILE = new File("data.json");

	private interface Permissions {
		String AUTHORIZED_CHANNELS = "authorizedChannels";
	}

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

	public static synchronized void storePermission(IGuild guild, IChannel channel) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));

			if(!mainObj.has(guild.getStringID())) {
				mainObj.put(guild.getStringID(), new JSONObject());
			}
			JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
			if(guildObj.has(Permissions.AUTHORIZED_CHANNELS)) {
				guildObj.put(Permissions.AUTHORIZED_CHANNELS, Storage.getAllowedChannels(guild).put(channel.getStringID()));
			} else {
				guildObj.put(Permissions.AUTHORIZED_CHANNELS, new JSONArray().put(channel.getStringID()));
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

	public static synchronized void storeCoins(IGuild guild, IUser user, int coins) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));

			if(!mainObj.has(guild.getStringID())) {
				mainObj.put(guild.getStringID(), new JSONObject());
			}
			JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
			guildObj.put(user.getStringID(), Integer.toString(coins));

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

	public static synchronized JSONArray getAllowedChannels(IGuild guild) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));
			if(mainObj.has(guild.getStringID())) {
				JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
				if(guildObj.has(Permissions.AUTHORIZED_CHANNELS)) {
					return guildObj.getJSONArray(Permissions.AUTHORIZED_CHANNELS);
				}
			}
		} catch (IOException e) {
			Log.error("Error while reading data file.", e);
		}
		return null;
	}

	public static synchronized int getCoins(IGuild guild, IUser user) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new String(Files.readAllBytes(Paths.get(DATA_FILE.getPath())), StandardCharsets.UTF_8));
			if(mainObj.has(guild.getStringID())) {
				JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
				if(guildObj.has(user.getStringID())) {
					return Integer.parseInt(guildObj.getString(user.getStringID()));
				}
			}
		} catch (IOException e) {
			Log.error("Error while reading data file.", e);
		}
		return 0;
	}

	public static synchronized String getApiKey(ApiKeys key) {
		try {
			JSONObject obj = new JSONObject(new String(Files.readAllBytes(Paths.get(API_KEYS_FILE.getPath())), StandardCharsets.UTF_8));
			return obj.getString(key.toString());
		} catch (IOException e) {
			Log.error("Error while accessing to API keys file.", e);
		}
		return null;
	}
}
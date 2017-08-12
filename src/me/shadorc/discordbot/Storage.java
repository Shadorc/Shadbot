package me.shadorc.discordbot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File API_KEYS_FILE = new File("api_keys.json");
	private static final File DATA_FILE = new File("data.json");

	public interface Setting {
		String ALLOWED_CHANNELS = "allowed_channels";
		String PREFIX = "prefix";
	}

	public enum ApiKeys {
		GIPHY_API_KEY,
		DTC_API_KEY,
		DISCORD_TOKEN,
		TWITTER_API_KEY,
		TWITTER_API_SECRET,
		TWITTER_TOKEN,
		TWITTER_TOKEN_SECRET,
		STEAM_API_KEY,
		OPENWEATHERMAP_API_KEY,
		DEVIANTART_CLIENT_ID,
		DEVIANTART_API_SECRET
	}

	private static void init() {
		if(!DATA_FILE.exists()) {
			FileWriter writer = null;
			try {
				DATA_FILE.createNewFile();
				writer = new FileWriter(DATA_FILE);
				writer.write(new JSONObject().toString());
				writer.flush();

			} catch (IOException e) {
				Log.error("Error while saving data file during initialization.", e);

			} finally {
				try {
					if(writer != null) {
						writer.close();
					}
				} catch (IOException e) {
					Log.error("Error while closing writer during initialization.", e);
				}
			}
		}
	}

	private static JSONObject getNewGuildObject() {
		JSONObject guildObj = new JSONObject();
		guildObj.put(Setting.PREFIX, Config.DEFAULT_PREFIX);
		return guildObj;
	}

	public static synchronized void storeSetting(IGuild guild, String setting, Object value) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));

			if(!mainObj.has(guild.getStringID())) {
				mainObj.put(guild.getStringID(), Storage.getNewGuildObject());
			}

			JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
			guildObj.put(setting, value);

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(2));
			writer.flush();

		} catch (IOException e) {
			Log.error("Error while saving setting.", e);

		} finally {
			try {
				if(writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				Log.error("Error while closing writer during the saving of the setting.", e);
			}
		}
	}

	public static synchronized void storeUser(User user) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));

			if(!mainObj.has(user.getGuild().getStringID())) {
				mainObj.put(user.getGuild().getStringID(), Storage.getNewGuildObject());
			}

			JSONObject guildObj = mainObj.getJSONObject(user.getGuild().getStringID());
			guildObj.put(user.getStringID(), user.toJSON());

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(2));
			writer.flush();

		} catch (IOException e) {
			Log.error("Error while saving user.", e);

		} finally {
			try {
				if(writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				Log.error("Error while closing writer during the saving of the user.", e);
			}
		}
	}

	public static synchronized Object getSetting(IGuild guild, String setting) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
			if(mainObj.has(guild.getStringID())) {
				JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
				if(guildObj.has(setting)) {
					return guildObj.get(setting);
				}
			}
		} catch (IOException e) {
			Log.error("Error while reading data file.", e);
		}
		return null;
	}

	public static synchronized User getUser(IGuild guild, IUser user) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
			if(mainObj.has(guild.getStringID())) {
				JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
				if(guildObj.has(user.getStringID())) {
					return new User(guild, user, guildObj.getJSONObject(user.getStringID()));
				}
			}
		} catch (IOException e) {
			Log.error("Error while reading data file.", e);
		}

		return new User(guild, user);
	}

	public static synchronized String getApiKey(ApiKeys key) {
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(API_KEYS_FILE.toURI().toURL().openStream()));
			if(mainObj.has(key.toString())) {
				return mainObj.getString(key.toString());
			}
		} catch (IOException e) {
			Log.error("Error while reading API keys file.", e);
		}
		return null;
	}
}
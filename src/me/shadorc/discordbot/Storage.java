package me.shadorc.discordbot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File API_KEYS_FILE = new File("api_keys.json");
	private static final File DATA_FILE = new File("data.json");

	public enum Setting {
		ALLOWED_CHANNELS("allowed_channels"),
		PREFIX("prefix");

		private final String key;

		Setting(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return key;
		}
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
		DEVIANTART_API_SECRET,
		BOTS_DISCORD_PW_TOKEN,
		DISCORD_BOTS_ORG_TOKEN
	}

	private static void init() {
		if(DATA_FILE.exists()) {
			return;
		}

		FileWriter writer = null;
		try {
			DATA_FILE.createNewFile();
			writer = new FileWriter(DATA_FILE);
			writer.write(new JSONObject().toString());
			writer.flush();

		} catch (IOException e) {
			LogUtils.error("An error occured during data file initialization.", e);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private static JSONObject getNewGuildObject() {
		JSONObject guildObj = new JSONObject();
		guildObj.put(Setting.PREFIX.toString(), Config.DEFAULT_PREFIX);
		return guildObj;
	}

	public static synchronized void storeSetting(IGuild guild, Setting setting, Object value) {
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
			guildObj.put(setting.toString(), value);

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(2));
			writer.flush();

		} catch (IOException e) {
			LogUtils.error("Error while saving setting.", e);

		} finally {
			IOUtils.closeQuietly(writer);
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
			LogUtils.error("Error while saving user.", e);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public static synchronized Object getSetting(IGuild guild, Setting setting) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		// TODO: Remove
		if(guild == null) {
			LogUtils.warn("Somewhere, womething very strange happened... Shadbot tried to get a setting from a non-existent guild... (Setting: " + setting.toString() + ")");
			return "/";
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));

			if(!mainObj.has(guild.getStringID())) {
				mainObj.put(guild.getStringID(), Storage.getNewGuildObject());
			}

			JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
			if(guildObj.has(setting.toString())) {
				return guildObj.get(setting.toString());
			}
		} catch (IOException e) {
			LogUtils.error("Error while reading data file.", e);
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
			LogUtils.error("Error while reading data file.", e);
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
			LogUtils.error("Error while reading API keys file.", e);
		}
		return null;
	}
}
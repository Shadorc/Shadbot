package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	public static final String USERS = "users";
	public static final String SETTINGS = "settings";
	public static final String COINS = "coins";

	private static final File DATA_FILE = new File("data.json");

	@SuppressWarnings("ucd")
	private static JSONObject saveObject;

	static {
		if(!DATA_FILE.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(DATA_FILE);
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occured during data file initialization. Exiting.", err);
				System.exit(1);

			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		try {
			saveObject = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occured during data file initialization. Exiting.", err);
			System.exit(1);
		}
	}

	public static JSONObject getUsers(IGuild guild) {
		return Storage.getOrInit(guild, USERS);
	}

	public static JSONObject getUser(IGuild guild, IUser user) {
		JSONObject userObj = Storage.getUsers(guild).optJSONObject(user.getStringID());
		if(userObj == null) {
			return new JSONObject().put(COINS, 0);
		}
		return userObj;
	}

	public static Object getSetting(IGuild guild, Setting setting) {
		Object value = Storage.getOrInit(guild, SETTINGS).opt(setting.toString());
		if(value == null) {
			return Storage.getDefaultSetting(setting);
		}
		return value;
	}

	public static int getCoins(IGuild guild, IUser user) {
		return Storage.getUser(guild, user).getInt(COINS);
	}

	public static void addCoins(IGuild guild, IUser user, int gains) {
		int coins = (int) Math.max(0, Math.min(Config.MAX_COINS, (long) (Storage.getCoins(guild, user) + gains)));
		Storage.setOrInit(guild, USERS, user.getStringID(), Storage.getUser(guild, user).put(COINS, coins));
	}

	public static void setSetting(IGuild guild, Setting setting, Object value) {
		// If new value equals the default one, remove setting from data file
		if(Storage.getDefaultSetting(setting) != null && value.toString().equals(Storage.getDefaultSetting(setting).toString())) {
			Storage.removeSetting(guild, setting);
		} else {
			Storage.setOrInit(guild, SETTINGS, setting.toString(), value);
		}
	}

	public synchronized static void removeSetting(IGuild guild, Setting setting) {
		JSONObject guildObj = saveObject.optJSONObject(guild.getStringID());
		if(guildObj == null || !guildObj.has(SETTINGS)) {
			return;
		}

		guildObj.getJSONObject(SETTINGS).remove(setting.toString());

		// If there is no more settings saved, remove it
		if(guildObj.getJSONObject(SETTINGS).length() == 0) {
			guildObj.remove(SETTINGS);
		}

		// If guild contains no more data, remove it
		if(guildObj.length() == 0) {
			saveObject.remove(guild.getStringID());
		} else {
			saveObject.put(guild.getStringID(), guildObj);
		}
	}

	private static Object getDefaultSetting(Setting setting) {
		switch (setting) {
			case ALLOWED_CHANNELS:
			case BLACKLIST:
				return new JSONArray();
			case DEFAULT_VOLUME:
				return Config.DEFAULT_VOLUME;
			case PREFIX:
				return Config.DEFAULT_PREFIX;
			default:
				return null;
		}
	}

	private synchronized static JSONObject getOrInit(IGuild guild, String setting) {
		JSONObject guildObj = saveObject.optJSONObject(guild.getStringID());
		if(guildObj == null) {
			guildObj = new JSONObject();
		}

		JSONObject jsonObj = guildObj.optJSONObject(setting);
		if(jsonObj == null) {
			jsonObj = new JSONObject();
		}
		return jsonObj;
	}

	private synchronized static void setOrInit(IGuild guild, String setting, String key, Object value) {
		JSONObject guildObj = saveObject.optJSONObject(guild.getStringID());
		if(guildObj == null) {
			guildObj = new JSONObject();
		}

		JSONObject jsonObj = guildObj.optJSONObject(setting);
		if(jsonObj == null) {
			jsonObj = new JSONObject();
		}

		jsonObj.put(key, value);
		guildObj.put(setting, jsonObj);
		saveObject.put(guild.getStringID(), guildObj);
	}

	public synchronized static void save() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(DATA_FILE);
			writer.write(saveObject.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving data !", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}
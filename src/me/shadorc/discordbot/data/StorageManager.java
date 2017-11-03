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

public class StorageManager {

	public static final String USERS = "users";
	public static final String SETTINGS = "settings";
	public static final String COINS = "coins";

	private static final File USER_DATA_FILE = new File("user_data.json");

	@SuppressWarnings("ucd")
	private static JSONObject userDataObj;

	static {
		if(!USER_DATA_FILE.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(USER_DATA_FILE);
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
			userDataObj = new JSONObject(new JSONTokener(USER_DATA_FILE.toURI().toURL().openStream()));
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occured during data file initialization. Exiting.", err);
			System.exit(1);
		}
	}

	public static JSONObject getUsers(IGuild guild) {
		return StorageManager.getOrInit(guild, USERS);
	}

	public static JSONObject getUser(IGuild guild, IUser user) {
		JSONObject userObj = StorageManager.getUsers(guild).optJSONObject(user.getStringID());
		if(userObj == null) {
			return new JSONObject().put(COINS, 0);
		}
		return userObj;
	}

	public static Object getSetting(IGuild guild, Setting setting) {
		Object value = StorageManager.getOrInit(guild, SETTINGS).opt(setting.toString());
		if(value == null) {
			return StorageManager.getDefaultSetting(setting);
		}
		return value;
	}

	public static int getCoins(IGuild guild, IUser user) {
		return StorageManager.getUser(guild, user).getInt(COINS);
	}

	public static void addCoins(IGuild guild, IUser user, int gains) {
		int coins = (int) Math.max(0, Math.min(Config.MAX_COINS, (long) (StorageManager.getCoins(guild, user) + gains)));
		StorageManager.setOrInit(guild, USERS, user.getStringID(), StorageManager.getUser(guild, user).put(COINS, coins));
	}

	public static void setSetting(IGuild guild, Setting setting, Object value) {
		// If new value equals the default one, remove setting from data file
		if(StorageManager.getDefaultSetting(setting) != null && value.toString().equals(StorageManager.getDefaultSetting(setting).toString())) {
			StorageManager.removeSetting(guild, setting);
		} else {
			StorageManager.setOrInit(guild, SETTINGS, setting.toString(), value);
		}
	}

	public synchronized static void removeSetting(IGuild guild, Setting setting) {
		JSONObject guildObj = userDataObj.optJSONObject(guild.getStringID());
		if(guildObj == null || !guildObj.has(SETTINGS)) {
			return;
		}

		JSONObject settingsObj = guildObj.getJSONObject(SETTINGS);
		settingsObj.remove(setting.toString());

		// If there is no more settings saved, remove it
		if(settingsObj.length() == 0) {
			guildObj.remove(SETTINGS);
		} else {
			guildObj.put(SETTINGS, settingsObj);
		}

		// If guild contains no more data, remove it
		if(guildObj.length() == 0) {
			userDataObj.remove(guild.getStringID());
		} else {
			userDataObj.put(guild.getStringID(), guildObj);
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
		JSONObject guildObj = userDataObj.optJSONObject(guild.getStringID());
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
		JSONObject guildObj = userDataObj.optJSONObject(guild.getStringID());
		if(guildObj == null) {
			guildObj = new JSONObject();
		}

		JSONObject jsonObj = guildObj.optJSONObject(setting);
		if(jsonObj == null) {
			jsonObj = new JSONObject();
		}

		jsonObj.put(key, value);
		guildObj.put(setting, jsonObj);
		userDataObj.put(guild.getStringID(), guildObj);
	}

	public synchronized static void save() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(USER_DATA_FILE);
			writer.write(userDataObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving data !", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}
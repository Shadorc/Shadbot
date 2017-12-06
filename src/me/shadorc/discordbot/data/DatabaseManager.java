package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class DatabaseManager {

	private static final File USER_DATA_FILE = new File("user_data.json");

	@SuppressWarnings("ucd")
	private static JSONObject userDataObj;

	static {
		if(!USER_DATA_FILE.exists()) {
			try (FileWriter writer = new FileWriter(USER_DATA_FILE)) {
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occurred during database file initialization. Exiting.", err);
				System.exit(1);
			}
		}

		try (InputStream stream = USER_DATA_FILE.toURI().toURL().openStream()) {
			userDataObj = new JSONObject(new JSONTokener(stream));
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occurred during database file initialization. Exiting.", err);
			System.exit(1);
		}
	}

	public static JSONObject getUsers(IGuild guild) {
		return DatabaseManager.getOrInit(guild, JSONKey.USERS);
	}

	public static JSONObject getUser(IGuild guild, IUser user) {
		JSONObject userObj = DatabaseManager.getUsers(guild).optJSONObject(user.getStringID());
		if(userObj == null) {
			return new JSONObject().put(JSONKey.COINS.toString(), 0);
		}
		return userObj;
	}

	public static Object getSetting(IGuild guild, Setting setting) {
		Object value = DatabaseManager.getOrInit(guild, JSONKey.SETTINGS).opt(setting.toString());
		if(value == null) {
			return DatabaseManager.getDefaultSetting(setting);
		}
		return value;
	}

	public static int getCoins(IGuild guild, IUser user) {
		return DatabaseManager.getUser(guild, user).getInt(JSONKey.COINS.toString());
	}

	public static void addCoins(IGuild guild, IUser user, int gains) {
		int coins = (int) Math.max(0, Math.min(Config.MAX_COINS, (long) DatabaseManager.getCoins(guild, user) + gains));
		DatabaseManager.setOrInit(guild, JSONKey.USERS, user.getStringID(),
				DatabaseManager.getUser(guild, user).put(JSONKey.COINS.toString(), coins));
	}

	public static void addCoins(IChannel channel, IUser user, int gains) {
		long coins = (long) DatabaseManager.getCoins(channel.getGuild(), user) + gains;
		if(coins > Config.MAX_COINS) {
			BotUtils.sendMessage(Emoji.BANK + " Congratulations, you've reached the maximum number of coins allowed.", channel);
		}
		DatabaseManager.addCoins(channel.getGuild(), user, gains);
	}

	public static void setSetting(IGuild guild, Setting setting, Object value) {
		// If new value equals the default one, remove setting from data file
		if(DatabaseManager.getDefaultSetting(setting) != null
				&& value.toString().equals(DatabaseManager.getDefaultSetting(setting).toString())) {
			DatabaseManager.removeSetting(guild, setting);
		} else {
			DatabaseManager.setOrInit(guild, JSONKey.SETTINGS, setting.toString(), value);
		}
	}

	public synchronized static void removeSetting(IGuild guild, Setting setting) {
		JSONObject guildObj = userDataObj.optJSONObject(guild.getStringID());
		if(guildObj == null || !guildObj.has(JSONKey.SETTINGS.toString())) {
			return;
		}

		JSONObject settingsObj = guildObj.getJSONObject(JSONKey.SETTINGS.toString());
		settingsObj.remove(setting.toString());

		// If there is no more settings saved, remove it
		if(settingsObj.length() == 0) {
			guildObj.remove(JSONKey.SETTINGS.toString());
		} else {
			guildObj.put(JSONKey.SETTINGS.toString(), settingsObj);
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

	private synchronized static JSONObject getOrInit(IGuild guild, JSONKey category) {
		JSONObject guildObj = userDataObj.optJSONObject(guild.getStringID());
		if(guildObj == null) {
			guildObj = new JSONObject();
		}

		JSONObject jsonObj = guildObj.optJSONObject(category.toString());
		if(jsonObj == null) {
			jsonObj = new JSONObject();
		}
		return jsonObj;
	}

	private synchronized static void setOrInit(IGuild guild, JSONKey category, String key, Object value) {
		JSONObject guildObj = userDataObj.optJSONObject(guild.getStringID());
		if(guildObj == null) {
			guildObj = new JSONObject();
		}

		JSONObject jsonObj = guildObj.optJSONObject(category.toString());
		if(jsonObj == null) {
			jsonObj = new JSONObject();
		}

		jsonObj.put(key, value);
		guildObj.put(category.toString(), jsonObj);
		userDataObj.put(guild.getStringID(), guildObj);
	}

	public synchronized static void save() {
		LogUtils.info("Saving database...");
		try (FileWriter writer = new FileWriter(USER_DATA_FILE)) {
			writer.write(userDataObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving database.", err);
		}
		LogUtils.info("Database saved.");
	}
}
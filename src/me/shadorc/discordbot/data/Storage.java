package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File DATA_FILE = new File("data.json");
	private static final ConcurrentHashMap<String, JSONObject> GUILDS_MAP = new ConcurrentHashMap<>();

	public enum DataCategory {
		SETTINGS("settings"),
		USERS("users");

		private final String name;

		DataCategory(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

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
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
			for(Object guildID : mainObj.keySet()) {
				GUILDS_MAP.put(guildID.toString(), mainObj.getJSONObject(guildID.toString()));
			}

		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("Error while reading data file. Exiting.", err);
			System.exit(1);
		}
	}

	private static JSONObject getGuildObject(IGuild guild) {
		return GUILDS_MAP.getOrDefault(guild.getStringID(), Storage.getDefaultGuildObject());
	}

	private static JSONObject getDefaultGuildObject() {
		JSONObject guildObj = new JSONObject();
		guildObj.put(DataCategory.SETTINGS.toString(), new JSONObject()
				.put(Setting.ALLOWED_CHANNELS.toString(), new JSONArray())
				.put(Setting.PREFIX.toString(), Config.DEFAULT_PREFIX)
				.put(Setting.DEFAULT_VOLUME.toString(), Config.DEFAULT_VOLUME));
		guildObj.put(DataCategory.USERS.toString(), new JSONArray());
		return guildObj;
	}

	public static JSONObject getSettings(IGuild guild) {
		return Storage.getGuildObject(guild).getJSONObject(DataCategory.SETTINGS.toString());
	}

	public static JSONArray getPlayers(IGuild guild) {
		return Storage.getGuildObject(guild).getJSONArray(DataCategory.USERS.toString());
	}

	public static Object getSetting(IGuild guild, Setting setting) {
		return Storage.getSettings(guild).opt(setting.toString());
	}

	public static DBUser getPlayer(IGuild guild, IUser user) {
		return new DBUser(guild, user, Storage.getUserObject(guild, user));
	}

	private static JSONObject getUserObject(IGuild guild, IUser user) {
		JSONArray array = Storage.getPlayers(guild);
		for(int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);
			if(obj.getLong("userID") == user.getLongID()) {
				return obj;
			}
		}
		return null;
	}

	public static void saveSetting(IGuild guild, Setting setting, Object value) {
		JSONObject settingsObj = Storage.getSettings(guild).put(setting.toString(), value);
		GUILDS_MAP.put(guild.getStringID(), Storage.getGuildObject(guild).put(DataCategory.SETTINGS.toString(), settingsObj));
	}

	public static void savePlayer(DBUser player) {
		JSONArray usersArray = Storage.getGuildObject(player.getGuild()).getJSONArray(DataCategory.USERS.toString()).put(player.toJSON());
		GUILDS_MAP.put(player.getGuild().getStringID(), Storage.getGuildObject(player.getGuild()).put(DataCategory.USERS.toString(), usersArray));
	}

	public static void removeSetting(IGuild guild, Setting setting) {
		Storage.getSettings(guild).remove(setting.toString());
	}

	public static void save() {
		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject();
			for(String guildId : GUILDS_MAP.keySet()) {
				mainObj.put(guildId, GUILDS_MAP.get(guildId));
			}

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving data !", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}
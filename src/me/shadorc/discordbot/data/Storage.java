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

import me.shadorc.discordbot.Player;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File DATA_FILE = new File("data.json");
	private static final ConcurrentHashMap<String, JSONObject> GUILDS_MAP = new ConcurrentHashMap<>();

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

	public enum Setting {
		ALLOWED_CHANNELS("allowed_channels"),
		PREFIX("prefix"),
		DEFAULT_VOLUME("default_volume"),
		AUTO_MESSAGE("auto_message"),
		JOIN_MESSAGE("join_message"),
		LEAVE_MESSAGE("leave_message"),
		MESSAGE_CHANNEL_ID("message_channel_id"),
		NSFW("nsfw");

		private final String key;

		Setting(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return key;
		}
	}

	private static JSONObject getGuild(IGuild guild) {
		return GUILDS_MAP.getOrDefault(guild.getStringID(), Storage.getDefaultGuildObject());
	}

	private static JSONObject getDefaultGuildObject() {
		JSONObject guildObj = new JSONObject();
		guildObj.put(Setting.ALLOWED_CHANNELS.toString(), new JSONArray());
		guildObj.put(Setting.PREFIX.toString(), Config.DEFAULT_PREFIX);
		guildObj.put(Setting.DEFAULT_VOLUME.toString(), Config.DEFAULT_VOLUME);
		return guildObj;
	}

	public static void saveSetting(IGuild guild, Setting setting, Object value) {
		GUILDS_MAP.put(guild.getStringID(), Storage.getGuild(guild).put(setting.toString(), value));
	}

	public static void savePlayer(Player player) {
		GUILDS_MAP.put(player.getGuild().getStringID(), Storage.getGuild(player.getGuild()).put(player.getUser().getStringID(), player.toJSON()));
	}

	public static Object getSetting(IGuild guild, Setting setting) {
		return Storage.getGuild(guild).opt(setting.toString());
	}

	public static Player getPlayer(IGuild guild, IUser user) {
		return new Player(guild, user, Storage.getGuild(guild).optJSONObject(user.getStringID()));
	}

	public static void removeSetting(IGuild guild, Setting setting) {
		Storage.getGuild(guild).remove(setting.toString());
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
package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.Player;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File DATA_FILE = new File("data.json");

	public enum Setting {
		ALLOWED_CHANNELS("allowed_channels"),
		PREFIX("prefix"),
		DEFAULT_VOLUME("default_volume");

		private final String key;

		Setting(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return key;
		}
	}

	public static void init() {
		FileWriter writer = null;
		try {
			DATA_FILE.createNewFile();
			writer = new FileWriter(DATA_FILE);
			writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("An error occured during data file initialization.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private static JSONObject getNewGuildObject() {
		JSONObject guildObj = new JSONObject();
		guildObj.put(Setting.ALLOWED_CHANNELS.toString(), new JSONArray());
		guildObj.put(Setting.PREFIX.toString(), Config.DEFAULT_PREFIX);
		guildObj.put(Setting.DEFAULT_VOLUME.toString(), Config.DEFAULT_VOLUME);
		return guildObj;
	}

	public static synchronized void saveSetting(IGuild guild, Setting setting, Object value) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));

			String guildID = guild.getStringID();
			JSONObject guildObj = mainObj.has(guildID) ? mainObj.getJSONObject(guildID) : Storage.getNewGuildObject();
			guildObj.put(setting.toString(), value);
			mainObj.put(guildID, guildObj);

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving setting.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public static synchronized void savePlayer(Player player) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));

			String guildID = player.getGuild().getStringID();
			JSONObject guildObj = mainObj.has(guildID) ? mainObj.getJSONObject(guildID) : Storage.getNewGuildObject();
			guildObj.put(player.getUser().getStringID(), player.toJSON());
			mainObj.put(guildID, guildObj);

			writer = new FileWriter(DATA_FILE);
			writer.write(mainObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving player.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public static synchronized Object getSetting(IGuild guild, Setting setting) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));

			String guildID = guild.getStringID();
			JSONObject guildObj = mainObj.has(guildID) ? mainObj.getJSONObject(guildID) : Storage.getNewGuildObject();

			return guildObj.opt(setting.toString());

		} catch (IOException err) {
			LogUtils.error("Error while reading data file.", err);
		}

		return null;
	}

	public static synchronized Player getPlayer(IGuild guild, IUser user) {
		if(!DATA_FILE.exists()) {
			Storage.init();
		}

		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
			if(mainObj.has(guild.getStringID())) {
				JSONObject guildObj = mainObj.getJSONObject(guild.getStringID());
				if(guildObj.has(user.getStringID())) {
					return new Player(guild, user, guildObj.getJSONObject(user.getStringID()));
				}
			}

		} catch (IOException err) {
			LogUtils.error("Error while reading data file.", err);
		}

		return new Player(guild, user);
	}
}
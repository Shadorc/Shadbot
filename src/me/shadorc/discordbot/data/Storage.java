package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Storage {

	private static final File DATA_FILE = new File("data.json");
	private static final ConcurrentHashMap<Long, DBGuild> GUILDS_MAP = new ConcurrentHashMap<>();
	static {
		if(DATA_FILE.exists()) {
			try {
				JSONObject mainObj = new JSONObject(new JSONTokener(DATA_FILE.toURI().toURL().openStream()));
				for(Object guildIDObj : mainObj.keySet()) {
					Long guildID = Long.parseLong(guildIDObj.toString());
					GUILDS_MAP.put(guildID, new DBGuild(Shadbot.getClient().getGuildByID(guildID), mainObj.getJSONObject(guildID.toString())));
				}

			} catch (JSONException | IOException err) {
				LogUtils.LOGGER.error("Error while reading data file. Exiting.", err);
				System.exit(1);
			}

		} else {
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
	}

	public static DBGuild getGuild(IGuild guild) {
		return GUILDS_MAP.getOrDefault(guild.getLongID(), new DBGuild(guild));
	}

	public static DBUser getUser(IGuild guild, IUser user) {
		return Storage.getGuild(guild).getUser(user.getLongID());
	}

	public static void saveGuild(DBGuild guild) {
		GUILDS_MAP.put(guild.getGuild().getLongID(), guild);
	}

	public static void saveUser(DBUser user) {
		Storage.getGuild(user.getGuild()).saveUser(user);
	}

	public static void save() {
		FileWriter writer = null;
		try {
			JSONObject mainObj = new JSONObject();
			for(Long guildId : GUILDS_MAP.keySet()) {
				mainObj.put(guildId.toString(), GUILDS_MAP.get(guildId).toJSON());
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
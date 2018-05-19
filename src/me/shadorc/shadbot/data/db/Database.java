package me.shadorc.shadbot.data.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.stats.DatabaseStatsManager;
import me.shadorc.shadbot.data.stats.DatabaseStatsManager.DatabaseEnum;

public class Database {

	private static final String FILE_NAME = "user_data.json";
	private static final File FILE = new File(DataManager.SAVE_DIR, FILE_NAME);

	private static JSONObject dbObject;

	@DataInit
	public static void init() throws JSONException, IOException {
		if(!FILE.exists()) {
			try (FileWriter writer = new FileWriter(FILE)) {
				writer.write(new JSONObject().toString(Config.JSON_INDENT_FACTOR));
			}
		}

		try (InputStream stream = FILE.toURI().toURL().openStream()) {
			dbObject = new JSONObject(new JSONTokener(stream));
		}
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 15, period = 15, unit = TimeUnit.MINUTES)
	public static void save() throws JSONException, IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(dbObject.toString(Config.JSON_INDENT_FACTOR));
		}
	}

	public static DBGuild getDBGuild(Snowflake guildId) {
		return new DBGuild(guildId);
	}

	public static DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
		return new DBMember(guildId, memberId);
	}

	public synchronized static JSONObject opt(String key) {
		return dbObject.optJSONObject(key);
	}

	public synchronized static void save(DBGuild dbGuild) {
		dbObject.put(dbGuild.getId().asString(), dbGuild.toJSON());
		DatabaseStatsManager.log(DatabaseEnum.GUILD_SAVED);
	}

	public synchronized static void save(DBMember dbUser) {
		JSONObject guildObj = dbObject.optJSONObject(dbUser.getGuildId().asString());
		if(guildObj == null) {
			guildObj = new JSONObject();
		}

		if(!guildObj.has(DBGuild.USERS_KEY)) {
			guildObj.put(DBGuild.USERS_KEY, new JSONObject());
		}

		guildObj.getJSONObject(DBGuild.USERS_KEY)
				.put(dbUser.getId().asString(), dbUser.toJSON());

		dbObject.put(dbUser.getGuildId().asString(), guildObj);
		DatabaseStatsManager.log(DatabaseEnum.USER_SAVED);
	}
}

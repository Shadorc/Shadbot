package me.shadorc.shadbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.data.Config;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DBUser;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Database {

	private static final String FILE_NAME = "user_data.json";
	private static final File FILE = new File(FILE_NAME);

	private static JSONObject dbObject;

	@DataInit
	public static void init() throws JSONException, IOException {
		if(!FILE.exists()) {
			try (FileWriter writer = new FileWriter(FILE)) {
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
			}
		}

		try (InputStream stream = FILE.toURI().toURL().openStream()) {
			dbObject = new JSONObject(new JSONTokener(stream));
		}
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 5, period = 5, unit = TimeUnit.MINUTES)
	public static void save() throws JSONException, IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(dbObject.toString(Config.INDENT_FACTOR));
		}
	}

	public static JSONObject getJSON() {
		return dbObject;
	}

	public static DBGuild getDBGuild(IGuild guild) {
		return new DBGuild(guild);
	}

	public static DBUser getDBUser(IGuild guild, IUser user) {
		return new DBUser(guild, user.getLongID());
	}

	public static JSONObject opt(String key) {
		return dbObject.optJSONObject(key);
	}
}

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
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DBUser;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Database extends AbstractData {

	private static final File DB_FILE = new File("user_data.json");

	private JSONObject dbObject;

	public Database() {
		super(DB_FILE, 5, 5, TimeUnit.MINUTES);

		if(!DB_FILE.exists()) {
			try (FileWriter writer = new FileWriter(DB_FILE)) {
				writer.write(new JSONObject().toString(Config.INDENT_FACTOR));
			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occurred during database file creation. Exiting.", err);
				System.exit(1);
			}
		}

		try (InputStream stream = DB_FILE.toURI().toURL().openStream()) {
			dbObject = new JSONObject(new JSONTokener(stream));
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occurred during database file initialisation. Exiting.", err);
			System.exit(1);
		}
	}

	public DBGuild getDBGuild(IGuild guild) {
		return new DBGuild(guild);
	}

	public DBUser getDBUser(IGuild guild, IUser user) {
		return new DBUser(guild, user.getLongID());
	}

	public JSONObject opt(String key) {
		return dbObject.optJSONObject(key);
	}

	@Override
	public JSONObject getJSON() {
		return dbObject;
	}
}

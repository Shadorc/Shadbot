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

public class LottoDataManager {

	public static final String POOL = "pool";

	public static final String USERS = "users";
	public static final String USER_ID = "userID";
	public static final String GUILD_ID = "guildID";
	public static final String NUM = "num";

	public static final String HISTORIC = "historic";
	public static final String HISTORIC_POOL = "historicPool";
	public static final String HISTORIC_WINNERS_COUNT = "historicWinnerCount";
	public static final String HISTORIC_NUM = "historicNum";

	private static final File LOTTERY_DATA_FILE = new File("lotto_data.json");

	@SuppressWarnings("ucd")
	private static JSONObject dataObj;

	static {
		if(!LOTTERY_DATA_FILE.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(LOTTERY_DATA_FILE);
				JSONObject defaultObj = new JSONObject();
				defaultObj.put(USERS, new JSONArray());
				defaultObj.put(POOL, 0);
				writer.write(defaultObj.toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occurred during lotto data file creation. Exiting.", err);
				System.exit(1);

			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		try {
			dataObj = new JSONObject(new JSONTokener(LOTTERY_DATA_FILE.toURI().toURL().openStream()));
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occurred during lotto data file initialization. Exiting.", err);
			System.exit(1);
		}
	}

	public static void addToPool(int coins) {
		int pool = (int) Math.max(0, Math.min(Config.MAX_COINS, (long) dataObj.optInt(POOL) + coins));
		dataObj.put(POOL, pool);
	}

	public static void addPlayer(IGuild guild, IUser user, int num) {
		JSONObject playerObj = new JSONObject()
				.put(GUILD_ID, guild.getLongID())
				.put(USER_ID, user.getLongID())
				.put(NUM, num);
		dataObj.getJSONArray(USERS).put(playerObj);
	}

	public static int getPool() {
		return dataObj.getInt(POOL);
	}

	public static JSONArray getPlayers() {
		return dataObj.getJSONArray(USERS);
	}

	public static JSONObject getHistoric() {
		return dataObj.optJSONObject(HISTORIC);
	}

	public static void setHistoric(int winnersCount, int pool, int num) {
		dataObj.put(HISTORIC, new JSONObject().put(HISTORIC_WINNERS_COUNT, winnersCount)
				.put(HISTORIC_POOL, pool)
				.put(HISTORIC_NUM, num));
	}

	public static void resetUsers() {
		dataObj.put(USERS, new JSONArray());
	}

	public static void resetPool() {
		dataObj.put(POOL, 0);
	}

	public static void save() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(LOTTERY_DATA_FILE);
			writer.write(dataObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving lotto data.", err);

		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}

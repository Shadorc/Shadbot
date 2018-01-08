package me.shadorc.shadbot.data.lotto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class LottoManager {

	private static final String HISTORIC = "historic";
	private static final String WINNERS_COUNT = "winnerCount";
	private static final String POOL = "pool";

	private static final String USERS = "users";
	private static final String NUM = "num";
	private static final String USER_ID = "userID";
	private static final String GUILD_ID = "guildID";

	private static final String FILE_NAME = "lotto_data.json";
	private static final File FILE = new File(FILE_NAME);

	private static JSONObject dataObj;

	@DataInit
	public static void init() throws JSONException, IOException {
		if(!FILE.exists()) {
			try (FileWriter writer = new FileWriter(FILE)) {
				JSONObject defaultObj = new JSONObject()
						.put(USERS, new JSONArray())
						.put(POOL, 0);
				writer.write(defaultObj.toString(Config.JSON_INDENT_FACTOR));
			}
		}

		try (InputStream stream = FILE.toURI().toURL().openStream()) {
			dataObj = new JSONObject(new JSONTokener(stream));
		}
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 10, period = 10, unit = TimeUnit.MINUTES)
	public static void save() throws JSONException, IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(dataObj.toString(Config.JSON_INDENT_FACTOR));
		}
	}

	public static void reset() {
		dataObj.put(USERS, new JSONArray());
		dataObj.put(POOL, 0);
	}

	public static int getPool() {
		return dataObj.getInt(POOL);
	}

	public static List<LottoPlayer> getPlayers() {
		List<LottoPlayer> players = new ArrayList<>();
		JSONArray usersArray = dataObj.getJSONArray(USERS);
		for(int i = 0; i < usersArray.length(); i++) {
			JSONObject userObj = usersArray.getJSONObject(i);
			players.add(new LottoPlayer(userObj.getLong(GUILD_ID), userObj.getLong(USER_ID), userObj.getInt(NUM)));
		}
		return players;
	}

	public static LottoHistoric getHistoric() {
		JSONObject historicObj = dataObj.optJSONObject(HISTORIC);
		if(historicObj == null) {
			return null;
		}
		return new LottoHistoric(historicObj.getInt(POOL), historicObj.getInt(WINNERS_COUNT), historicObj.getInt(NUM));
	}

	public static synchronized void addToPool(int coins) {
		int newPool = dataObj.optInt(POOL) + (int) Math.ceil(coins / 100f);
		dataObj.put(POOL, Math.max(0, Math.min(Config.MAX_COINS, newPool)));
	}

	public static synchronized void addPlayer(IGuild guild, IUser user, int num) {
		JSONObject playerObj = new JSONObject()
				.put(GUILD_ID, guild.getLongID())
				.put(USER_ID, user.getLongID())
				.put(NUM, num);
		dataObj.getJSONArray(USERS).put(playerObj);
	}

	public static void setHistoric(int winnersCount, int pool, int num) {
		dataObj.put(HISTORIC, new JSONObject()
				.put(WINNERS_COUNT, winnersCount)
				.put(POOL, pool)
				.put(NUM, num));
	}
}

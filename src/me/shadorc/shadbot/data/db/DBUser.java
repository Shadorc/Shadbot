package me.shadorc.shadbot.data.db;

import org.json.JSONObject;

import me.shadorc.discordbot.data.Config;
import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.utils.JSONUtils;
import sx.blah.discord.handle.obj.IGuild;

public class DBUser {

	private static final String COINS_KEY = "coins";

	private final IGuild guild;
	private final long userID;
	private final JSONObject userObj;

	public DBUser(IGuild guild, long userID) {
		this.guild = guild;
		this.userID = userID;
		this.userObj = JSONUtils.getOrDefault(Database.getJSON(), new JSONObject(),
				guild.getStringID(), DBGuild.USERS_KEY, Long.toString(userID));
	}

	public IGuild getGuild() {
		return guild;
	}

	public long getUserID() {
		return userID;
	}

	public int getCoins() {
		if(userObj.has(COINS_KEY)) {
			return userObj.getInt(COINS_KEY);
		} else {
			return 0;
		}
	}

	public void addCoins(int gains) {
		userObj.put(COINS_KEY, Math.max(0, Math.min(Config.MAX_COINS, (long) this.getCoins() + gains)));
	}
}

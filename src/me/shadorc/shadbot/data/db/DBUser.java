package me.shadorc.shadbot.data.db;

import org.json.JSONObject;

import me.shadorc.shadbot.Config;
import sx.blah.discord.handle.obj.IGuild;

public class DBUser {

	private static final String COINS_KEY = "coins";

	private final IGuild guild;
	private final long userID;

	private int coins;

	public DBUser(IGuild guild, long userID) {
		this.guild = guild;
		this.userID = userID;

		this.load();
	}

	public final void load() {
		JSONObject guildObj = Database.opt(guild.getStringID());
		if(guildObj == null) {
			return;
		}

		JSONObject usersObj = guildObj.optJSONObject(DBGuild.USERS_KEY);
		if(usersObj == null) {
			return;
		}

		JSONObject userObj = usersObj.optJSONObject(Long.toString(userID));
		if(userObj == null) {
			return;
		}

		this.coins = userObj.optInt(COINS_KEY);
	}

	public IGuild getGuild() {
		return guild;
	}

	public long getUserID() {
		return userID;
	}

	public int getCoins() {
		return coins;
	}

	public void addCoins(int gains) {
		coins = (int) Math.max(0, Math.min(Config.MAX_COINS, (long) this.getCoins() + gains));
		Database.save(this);
	}

	public JSONObject toJSON() {
		JSONObject userObj = new JSONObject();
		if(coins != 0) {
			userObj.put(COINS_KEY, coins);
		}
		return userObj;
	}
}

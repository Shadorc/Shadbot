package me.shadorc.discordbot.data;

import org.json.JSONObject;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Player {

	private final IGuild guild;
	private final IUser user;

	private int coins;

	public Player(IGuild guild, IUser user, JSONObject userObj) {
		this.guild = guild;
		this.user = user;
		this.coins = userObj == null ? 0 : userObj.getInt("coins");
	}

	public IGuild getGuild() {
		return guild;
	}

	public IUser getUser() {
		return user;
	}

	public int getCoins() {
		return coins;
	}

	public void addCoins(int gains) {
		this.coins = (int) Math.max(0, Math.min(Config.MAX_COINS, (long) (this.coins + gains)));
		this.save();
	}

	public JSONObject toJSON() {
		JSONObject userJson = new JSONObject();
		userJson.put("userID", user.getLongID());
		userJson.put("coins", coins);
		return userJson;
	}

	private void save() {
		Storage.savePlayer(this);
	}
}

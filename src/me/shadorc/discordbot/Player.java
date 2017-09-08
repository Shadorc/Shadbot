package me.shadorc.discordbot;

import org.json.JSONObject;

import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Player {

	private final IGuild guild;
	private final IUser user;

	private int coins;

	public Player(IGuild guild, IUser user, JSONObject userObj) {
		this.guild = guild;
		this.user = user;
		this.coins = userObj.getInt("coins");
	}

	public Player(IGuild guild, IUser user) {
		this.guild = guild;
		this.user = user;
		this.coins = 0;
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
		try {
			this.coins = Math.addExact(coins, gains);
		} catch (ArithmeticException err) {
			this.coins = Integer.MAX_VALUE;
			LogUtils.warn("A user's money exceeded the maximum value of an Integer. (ID: " + user.getLongID() + ")");
		}
		this.save();
	}

	public JSONObject toJSON() {
		JSONObject userJson = new JSONObject();
		userJson.put("coins", coins);
		return userJson;
	}

	private void save() {
		Storage.savePlayer(this);
	}
}

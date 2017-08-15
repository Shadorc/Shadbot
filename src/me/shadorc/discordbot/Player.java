package me.shadorc.discordbot;

import org.json.JSONObject;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class Player {

	private final IGuild guild;
	private final IUser user;

	private int coins;

	public Player(IGuild guild, IUser user) {
		this.guild = guild;
		this.user = user;
		this.coins = 0;
	}

	public Player(IGuild guild, IUser user, JSONObject userObj) {
		this.guild = guild;
		this.user = user;
		this.coins = userObj.getInt("coins");
	}

	public IGuild getGuild() {
		return guild;
	}

	public String getStringID() {
		return user.getStringID();
	}

	public int getCoins() {
		return coins;
	}

	public void addCoins(int gains) {
		this.coins += gains;
		this.save();
	}

	public String mention() {
		return user.mention();
	}

	public JSONObject toJSON() {
		JSONObject userJson = new JSONObject();
		userJson.put("coins", coins);
		return userJson;
	}

	private void save() {
		Storage.storeUser(this);
	}
}

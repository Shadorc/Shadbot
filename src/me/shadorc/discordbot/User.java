package me.shadorc.discordbot;

import org.json.JSONObject;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class User {

	private final IGuild guild;
	private final IUser user;
	private int coins;
	private int level;

	public User(IGuild guild, IUser user) {
		this.guild = guild;
		this.user = user;
		this.coins = 0;
		this.level = 1;
	}

	public User(IGuild guild, long id, JSONObject obj) {
		this.guild = guild;
		this.user = guild.getUserByID(id);
		this.coins = obj.getInt("coins");
		this.level = obj.getInt("level");
	}

	public void addCoins(int gains) {
		this.coins += gains;
		this.save();
	}

	public void setCoins(int coins) {
		this.coins = coins;
		this.save();
	}

	public IGuild getGuild() {
		return guild;
	}

	public int getCoins() {
		return coins;
	}

	public int getLevel() {
		return level;
	}

	public String getStringID() {
		return user.getStringID();
	}

	public String mention() {
		return user.mention();
	}

	public JSONObject toJSON() {
		JSONObject userJson = new JSONObject();
		userJson.put("coins", coins);
		userJson.put("level", level);
		return userJson;
	}

	private void save() {
		Storage.storeUser(this);
	}
}

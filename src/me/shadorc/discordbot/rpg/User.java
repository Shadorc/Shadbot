package me.shadorc.discordbot.rpg;

import org.json.JSONObject;

import me.shadorc.discordbot.Storage;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class User {

	private final IGuild guild;
	private final IUser user;

	private final boolean isNew;

	private Weapon weapon;
	private int life;
	private int coins;
	private int level;
	private int xp;

	public User(IGuild guild, IUser user) {
		this.guild = guild;
		this.user = user;

		this.isNew = true;

		this.weapon = new Weapon();
		this.life = 100;
		this.coins = 0;
		this.level = 1;
		this.xp = 0;
	}

	public User(IGuild guild, long id, JSONObject obj) {
		this.guild = guild;
		this.user = guild.getUserByID(id);

		this.isNew = false;

		this.weapon = new Weapon(obj.getJSONObject("weapon"));
		this.life = obj.getInt("life");
		this.coins = obj.getInt("coins");
		this.level = obj.getInt("level");
		this.xp = obj.getInt("xp");
	}

	public IGuild getGuild() {
		return guild;
	}

	public Weapon getWeapon() {
		return weapon;
	}

	public int getLife() {
		return life;
	}

	public int getCoins() {
		return coins;
	}

	public int getLevel() {
		return level;
	}

	public int getXp() {
		return xp;
	}

	public int getXpToNextLevel() {
		return (int) Math.round(8.25 * level + 10);
	}

	public String getStringID() {
		return user.getStringID();
	}

	public void addCoins(int gains) {
		this.coins += gains;
		this.save();
	}

	public boolean addXp(int xp) {
		boolean leveledUp = false;
		int xpToUp = this.getXpToNextLevel();
		this.xp += xp;
		while(this.getXp() >= xpToUp) {
			this.xp -= xpToUp;
			this.level++;
			leveledUp = true;
		}
		this.save();
		return leveledUp;
	}

	public void setCoins(int coins) {
		this.coins = coins;
		this.save();
	}

	public void hurt(int dmg) {
		this.life = Math.max(0, life - dmg);
		this.save();
	}

	public String mention() {
		return user.mention();
	}

	public boolean isNew() {
		return isNew;
	}

	public JSONObject toJSON() {
		JSONObject userJson = new JSONObject();
		userJson.put("weapon", weapon.toJSON());
		userJson.put("life", life);
		userJson.put("coins", coins);
		userJson.put("level", level);
		userJson.put("xp", xp);
		return userJson;
	}

	private void save() {
		Storage.storeUser(this);
	}
}

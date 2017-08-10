package me.shadorc.discordbot.rpg;

import org.json.JSONObject;

public class Weapon {

	private final String name;
	private final int minDmg;
	private final int maxDmg;
	private final int durability;

	public Weapon() {
		this.name = "Arme standard";
		this.minDmg = 1;
		this.maxDmg = 3;
		this.durability = 100;
	}

	public Weapon(JSONObject jsonWeapon) {
		this.name = jsonWeapon.getString("name");
		this.minDmg = jsonWeapon.getInt("minDamage");
		this.maxDmg = jsonWeapon.getInt("maxDamage");
		this.durability = jsonWeapon.getInt("durability");
	}

	public String getName() {
		return name;
	}

	public int getMaxDamage() {
		return maxDmg;
	}

	public int getMinDamage() {
		return minDmg;
	}

	public int getDurability() {
		return durability;
	}

	public JSONObject toJSON() {
		JSONObject weaponJson = new JSONObject();
		weaponJson.put("name", name);
		weaponJson.put("minDamage", minDmg);
		weaponJson.put("maxDamage", maxDmg);
		weaponJson.put("durability", durability);
		return weaponJson;
	}
}

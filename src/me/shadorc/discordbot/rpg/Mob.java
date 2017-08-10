package me.shadorc.discordbot.rpg;

import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.utils.MathUtils;

public class Mob {

	private static final List<String> MOBS = Arrays.asList("Slime", "Araignée", "Loup", "Vampire", "Démon");

	private final String name;
	private final int level;
	private int life;

	public Mob(int userLevel) {
		this.name = MOBS.get(MathUtils.rand(MOBS.size()));
		this.level = MathUtils.rand(Math.max(1, userLevel - 2), userLevel + 2);
		this.life = userLevel * 5 + MathUtils.rand(userLevel * 2);
	}

	public String getName() {
		return name;
	}

	public int getDamage() {
		return level * 2 + MathUtils.rand(level);
	}

	public int getLife() {
		return life;
	}

	public int getLevel() {
		return level;
	}

	public void hurt(int dmg) {
		this.life = Math.max(0, life - dmg);
	}
}

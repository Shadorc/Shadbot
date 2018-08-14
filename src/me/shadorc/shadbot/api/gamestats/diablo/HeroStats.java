package me.shadorc.shadbot.api.gamestats.diablo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroStats {

	@JsonProperty("damage")
	private double damage;

	public double getDamage() {
		return damage;
	}

	@Override
	public String toString() {
		return String.format("HeroStats [damage=%s]", damage);
	}

}

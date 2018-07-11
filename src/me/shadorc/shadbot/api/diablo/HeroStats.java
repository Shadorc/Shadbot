package me.shadorc.shadbot.api.diablo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroStats {

	@JsonProperty("damage")
	private double damage;

	public double getDamage() {
		return damage;
	}

	@Override
	public String toString() {
		return "HeroStats [damage=" + damage + "]";
	}

}

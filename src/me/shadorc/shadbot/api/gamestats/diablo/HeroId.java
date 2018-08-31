package me.shadorc.shadbot.api.gamestats.diablo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroId {

	@JsonProperty("id")
	private long id;

	public long getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return String.format("HeroId [id=%s]", this.id);
	}

}
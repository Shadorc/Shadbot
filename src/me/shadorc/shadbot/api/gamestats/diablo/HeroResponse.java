package me.shadorc.shadbot.api.gamestats.diablo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.shadorc.shadbot.utils.StringUtils;

public class HeroResponse {

	@Nullable
	@JsonProperty("code")
	private String code;
	@JsonProperty("name")
	private String name;
	@JsonProperty("class")
	private String className;
	@JsonProperty("level")
	private int level;
	@JsonProperty("stats")
	private HeroStats stats;

	public String getCode() {
		return this.code;
	}

	public String getName() {
		return this.name;
	}

	public String getClassName() {
		return StringUtils.capitalize(this.className.replace("-", " "));
	}

	public int getLevel() {
		return this.level;
	}

	public HeroStats getStats() {
		return this.stats;
	}

	@Override
	public String toString() {
		return String.format("HeroResponse [code=%s, name=%s, className=%s, level=%s, stats=%s]", this.code, this.name, this.className, this.level, this.stats);
	}

}

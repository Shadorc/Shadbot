package me.shadorc.shadbot.api.diablo;

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
	private HeroStatsResponse stats;

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getClassName() {
		return StringUtils.capitalize(className.replace("-", " "));
	}

	public int getLevel() {
		return level;
	}

	public HeroStatsResponse getStats() {
		return stats;
	}

}

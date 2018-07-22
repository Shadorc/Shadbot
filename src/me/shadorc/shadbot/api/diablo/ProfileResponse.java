package me.shadorc.shadbot.api.diablo;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProfileResponse {

	@Nullable
	@JsonProperty("code")
	private String code;
	@JsonProperty("battleTag")
	private String battleTag;
	@JsonProperty("paragonLevel")
	private int paragonLevel;
	@JsonProperty("paragonLevelHardcore")
	private int paragonLevelHardcore;
	@JsonProperty("paragonLevelSeason")
	private int paragonLevelSeason;
	@JsonProperty("paragonLevelSeasonHardcore")
	private int paragonLevelSeasonHardcore;
	@JsonProperty("guildName")
	private String guildName;
	@JsonProperty("heroes")
	private List<HeroId> heroeIds;

	public String getCode() {
		return code;
	}

	public String getBattleTag() {
		return battleTag;
	}

	public int getParagonLevel() {
		return paragonLevel;
	}

	public int getParagonLevelHardcore() {
		return paragonLevelHardcore;
	}

	public int getParagonLevelSeason() {
		return paragonLevelSeason;
	}

	public int getParagonLevelSeasonHardcore() {
		return paragonLevelSeasonHardcore;
	}

	public String getGuildName() {
		return guildName;
	}

	public List<HeroId> getHeroeIds() {
		return heroeIds;
	}

	@Override
	public String toString() {
		return String.format("ProfileResponse [code=%s, battleTag=%s, paragonLevel=%s, paragonLevelHardcore=%s, paragonLevelSeason=%s, "
				+ "paragonLevelSeasonHardcore=%s, guildName=%s, heroeIds=%s]", code, battleTag, paragonLevel, paragonLevelHardcore, 
				paragonLevelSeason, paragonLevelSeasonHardcore, guildName, heroeIds);
	}

}

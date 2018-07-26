package me.shadorc.shadbot.api.fortnite;

import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

	@Nullable
	@JsonProperty("p2")
	private SubStats soloStats;
	@Nullable
	@JsonProperty("p10")
	private SubStats duoStats;
	@Nullable
	@JsonProperty("p9")
	private SubStats squadStats;
	@Nullable
	@JsonProperty("curr_p2")
	private SubStats seasonSoloStats;
	@Nullable
	@JsonProperty("curr_p10")
	private SubStats seasonDuoStats;
	@Nullable
	@JsonProperty("curr_p9")
	private SubStats seasonSquadStats;

	public SubStats getSoloStats() {
		return Objects.requireNonNullElse(soloStats, SubStats.DEFAULT);
	}

	public SubStats getDuoStats() {
		return Objects.requireNonNullElse(duoStats, SubStats.DEFAULT);
	}

	public SubStats getSquadStats() {
		return Objects.requireNonNullElse(squadStats, SubStats.DEFAULT);
	}

	public SubStats getSeasonSoloStats() {
		return Objects.requireNonNullElse(seasonSoloStats, SubStats.DEFAULT);
	}

	public SubStats getSeasonDuoStats() {
		return Objects.requireNonNullElse(seasonDuoStats, SubStats.DEFAULT);
	}

	public SubStats getSeasonSquadStats() {
		return Objects.requireNonNullElse(seasonSquadStats, SubStats.DEFAULT);
	}

	@Override
	public String toString() {
		return String.format("Stats [soloStats=%s, duoStats=%s, squadStats=%s, seasonSoloStats=%s, seasonDuoStats=%s, seasonSquadStats=%s]",
				soloStats, duoStats, squadStats, seasonSoloStats, seasonDuoStats, seasonSquadStats);
	}

}

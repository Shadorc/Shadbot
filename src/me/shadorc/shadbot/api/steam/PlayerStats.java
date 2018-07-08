package me.shadorc.shadbot.api.steam;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerStats {

	@Nullable
	@JsonProperty("stats")
	private List<StatResponse> stats;

	public List<StatResponse> getStats() {
		return stats;
	}

}

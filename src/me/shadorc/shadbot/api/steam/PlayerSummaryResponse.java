package me.shadorc.shadbot.api.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSummaryResponse {

	@JsonProperty("response")
	private PlayersResponse reponse;

	public PlayersResponse getResponse() {
		return reponse;
	}
}

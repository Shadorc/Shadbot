package me.shadorc.shadbot.api.diablo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileHeroResponse {

	@JsonProperty("id")
	private long id;

	public long getId() {
		return id;
	}

}

package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorResponse {

	@JsonProperty("username")
	private String username;

	public String getUsername() {
		return username;
	}

}

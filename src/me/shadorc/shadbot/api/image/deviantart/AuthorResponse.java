package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorResponse {

	@JsonProperty("username")
	private String username;

	public String getUsername() {
		return username;
	}

}

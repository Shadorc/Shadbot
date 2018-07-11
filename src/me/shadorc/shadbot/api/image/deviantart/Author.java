package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Author {

	@JsonProperty("username")
	private String username;

	public String getUsername() {
		return username;
	}

	@Override
	public String toString() {
		return "Author [username=" + username + "]";
	}

}

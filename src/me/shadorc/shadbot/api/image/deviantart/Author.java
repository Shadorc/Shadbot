package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Author {

	@JsonProperty("username")
	private String username;

	public String getUsername() {
		return this.username;
	}

	@Override
	public String toString() {
		return String.format("Author [username=%s]", this.username);
	}

}

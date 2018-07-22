package me.shadorc.shadbot.api.trivia.category;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriviaCategory {

	@JsonProperty("id")
	private int id;
	@JsonProperty("name")
	private String name;

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("TriviaCategory [id=%s, name=%s]", id, name);
	}

}

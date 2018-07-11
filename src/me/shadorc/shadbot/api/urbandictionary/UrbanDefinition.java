package me.shadorc.shadbot.api.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UrbanDefinition {

	@JsonProperty("definition")
	private String definition;
	@JsonProperty("example")
	private String example;
	@JsonProperty("word")
	private String word;
	@JsonProperty("permalink")
	private String permalink;

	public String getDefinition() {
		return definition;
	}

	public String getExample() {
		return example;
	}

	public String getWord() {
		return word;
	}

	public String getPermalink() {
		return permalink;
	}

	@Override
	public String toString() {
		return "UrbanDefinition [definition=" + definition
				+ ", example=" + example
				+ ", word=" + word
				+ ", permalink=" + permalink
				+ "]";
	}

}

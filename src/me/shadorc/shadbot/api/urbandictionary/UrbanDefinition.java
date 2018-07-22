package me.shadorc.shadbot.api.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.shadorc.shadbot.utils.StringUtils;

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
		return StringUtils.remove(definition, "[", "]");
	}

	public String getExample() {
		return StringUtils.remove(example, "[", "]");
	}

	public String getWord() {
		return word;
	}

	public String getPermalink() {
		return permalink;
	}

	@Override
	public String toString() {
		return String.format("UrbanDefinition [definition=%s, example=%s, word=%s, permalink=%s]", definition, example, word, permalink);
	}

}

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
		return StringUtils.remove(this.definition, "[", "]");
	}

	public String getExample() {
		return StringUtils.remove(this.example, "[", "]");
	}

	public String getWord() {
		return this.word;
	}

	public String getPermalink() {
		return this.permalink;
	}

	@Override
	public String toString() {
		return String.format("UrbanDefinition [definition=%s, example=%s, word=%s, permalink=%s]", this.definition, this.example, this.word, this.permalink);
	}

}

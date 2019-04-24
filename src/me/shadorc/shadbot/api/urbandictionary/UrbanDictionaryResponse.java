package me.shadorc.shadbot.api.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UrbanDictionaryResponse {

	@JsonProperty("list")
	private List<UrbanDefinition> definitions;

	public List<UrbanDefinition> getDefinitions() {
		return this.definitions;
	}

}

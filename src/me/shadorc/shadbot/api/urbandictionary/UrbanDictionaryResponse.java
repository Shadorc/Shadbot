package me.shadorc.shadbot.api.urbandictionary;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UrbanDictionaryResponse {

	@JsonProperty("list")
	private List<UrbanDefinition> definitions;

	public List<UrbanDefinition> getDefinitions() {
		return this.definitions;
	}

}

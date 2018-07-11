package me.shadorc.shadbot.api.urbandictionary;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UrbanDictionaryResponse {

	@JsonProperty("result_type")
	private String resultType;
	@JsonProperty("list")
	private List<UrbanDefinition> definitions;

	public String getResultType() {
		return resultType;
	}

	public List<UrbanDefinition> getDefinitions() {
		return definitions;
	}

	@Override
	public String toString() {
		return "UrbanDictionaryResponse [resultType=" + resultType
				+ ", definitions=" + definitions
				+ "]";
	}

}

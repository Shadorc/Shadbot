package com.locibot.locibot.api.json.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UrbanDictionaryResponse(@JsonProperty("list") List<UrbanDefinition> definitions) {

}

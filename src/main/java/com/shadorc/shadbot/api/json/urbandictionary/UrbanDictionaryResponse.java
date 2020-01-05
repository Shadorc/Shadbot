package com.shadorc.shadbot.api.json.urbandictionary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class UrbanDictionaryResponse {

    @JsonProperty("list")
    private List<UrbanDefinition> definitions;

    public List<UrbanDefinition> getDefinitions() {
        return Collections.unmodifiableList(this.definitions);
    }

}

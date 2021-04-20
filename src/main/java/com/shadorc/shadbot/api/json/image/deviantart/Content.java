package com.shadorc.shadbot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Content(@JsonProperty("src") String source) {

}

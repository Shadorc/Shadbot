package com.shadorc.shadbot.api.json.pandorabots;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatBotResponse {

    @JsonProperty("result")
    private ChatBotResult result;

    public ChatBotResult getResult() {
        return this.result;
    }
}

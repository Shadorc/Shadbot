package com.shadorc.shadbot.api.trivia;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class TriviaResponse {

    @JsonProperty("response_code")
    private int responseCode;
    @JsonProperty("results")
    private List<TriviaResult> results;

    public int getResponseCode() {
        return this.responseCode;
    }

    public List<TriviaResult> getResults() {
        return Collections.unmodifiableList(this.results);
    }

}

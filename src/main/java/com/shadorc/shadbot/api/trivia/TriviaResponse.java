package com.shadorc.shadbot.api.trivia;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class TriviaResponse {

    @JsonProperty("results")
    private List<TriviaResult> results;

    public List<TriviaResult> getResults() {
        return Collections.unmodifiableList(this.results);
    }

}

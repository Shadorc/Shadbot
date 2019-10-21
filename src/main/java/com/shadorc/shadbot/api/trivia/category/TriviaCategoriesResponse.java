package com.shadorc.shadbot.api.trivia.category;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class TriviaCategoriesResponse {

    @JsonProperty("trivia_categories")
    private List<TriviaCategory> categories;

    public List<Integer> getIds() {
        return this.categories.stream()
                .map(TriviaCategory::getId)
                .collect(Collectors.toList());
    }

    public List<String> getNames() {
        return this.categories.stream()
                .map(TriviaCategory::getName)
                .collect(Collectors.toList());
    }

}

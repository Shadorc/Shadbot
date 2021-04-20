package com.shadorc.shadbot.api.json.trivia.category;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TriviaCategoriesResponse(@JsonProperty("trivia_categories") List<TriviaCategory> categories) {

    public List<Integer> getIds() {
        return this.categories.stream()
                .map(TriviaCategory::id)
                .toList();
    }

    public List<String> getNames() {
        return this.categories.stream()
                .map(TriviaCategory::name)
                .toList();
    }

}

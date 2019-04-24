package me.shadorc.shadbot.api.trivia.category;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriviaCategory {

    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

}

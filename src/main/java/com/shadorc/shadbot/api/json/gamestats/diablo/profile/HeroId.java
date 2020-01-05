package com.shadorc.shadbot.api.json.gamestats.diablo.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroId {

    @JsonProperty("id")
    private long id;

    public long getId() {
        return this.id;
    }

}
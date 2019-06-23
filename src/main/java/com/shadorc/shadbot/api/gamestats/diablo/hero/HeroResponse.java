package com.shadorc.shadbot.api.gamestats.diablo.hero;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.StringUtils;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class HeroResponse {

    @Nullable
    @JsonProperty("code")
    private String code;
    @JsonProperty("name")
    private String name;
    @JsonProperty("class")
    private String className;
    @JsonProperty("level")
    private int level;
    @JsonProperty("stats")
    private HeroStats stats;

    public Optional<String> getCode() {
        return Optional.ofNullable(this.code);
    }

    public String getName() {
        return this.name;
    }

    public String getClassName() {
        return StringUtils.capitalize(this.className.replace("-", " "));
    }

    public int getLevel() {
        return this.level;
    }

    public HeroStats getStats() {
        return this.stats;
    }

}

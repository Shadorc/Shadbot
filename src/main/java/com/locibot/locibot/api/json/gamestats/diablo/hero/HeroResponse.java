package com.locibot.locibot.api.json.gamestats.diablo.hero;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.utils.StringUtil;

import java.util.Optional;

public record HeroResponse(Optional<String> code,
                           String name,
                           @JsonProperty("class") String className,
                           HeroStats stats) {

    public String getClassName() {
        return StringUtil.capitalize(this.className.replace("-", " "));
    }

}

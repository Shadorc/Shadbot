package com.shadorc.shadbot.api.json.gamestats.diablo.hero;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.StringUtil;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public record HeroResponse(@Nullable String code,
                           String name,
                           @JsonProperty("class") String className,
                           HeroStats stats) {

    public Optional<String> getCode() {
        return Optional.ofNullable(this.code);
    }

    public String getClassName() {
        return StringUtil.capitalize(this.className.replace("-", " "));
    }

}

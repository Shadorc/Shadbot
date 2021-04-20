package com.shadorc.shadbot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public record Image(@Nullable Content content,
                    Author author,
                    String url,
                    String title,
                    @JsonProperty("category_path") String categoryPath) {

    public Optional<Content> getContent() {
        return Optional.ofNullable(this.content);
    }

}

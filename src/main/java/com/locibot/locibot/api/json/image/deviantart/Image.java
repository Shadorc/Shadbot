package com.locibot.locibot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record Image(Optional<Content> content,
                    Author author,
                    String url,
                    String title,
                    @JsonProperty("category_path") String categoryPath) {

}

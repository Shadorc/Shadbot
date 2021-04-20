package com.shadorc.shadbot.api.json.wikipedia;

import reactor.util.annotation.Nullable;

import java.util.Optional;

public record WikipediaPage(String title,
                            @Nullable String extract) {

    public String getEncodedTitle() {
        return this.title.replace(" ", "_");
    }

    public Optional<String> getExtract() {
        return Optional.ofNullable(this.extract);
    }

}

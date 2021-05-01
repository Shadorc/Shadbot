package com.shadorc.shadbot.api.json.wikipedia;

import java.util.Optional;

public record WikipediaPage(String title,
                            Optional<String> extract) {

    public String getEncodedTitle() {
        return this.title.replace(" ", "_");
    }

}

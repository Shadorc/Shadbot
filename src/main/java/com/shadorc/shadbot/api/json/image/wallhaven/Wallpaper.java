package com.shadorc.shadbot.api.json.image.wallhaven;

import java.util.Optional;
import java.util.function.Predicate;

public record Wallpaper(String url,
                        String path,
                        String resolution,
                        String purity,
                        String source) {

    public Optional<String> getSource() {
        return Optional.of(this.source).
                filter(Predicate.not(String::isBlank));
    }

}

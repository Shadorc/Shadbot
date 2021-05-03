package com.shadorc.shadbot.api.json.genius;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Result(@JsonProperty("full_title") String fullTitle,
                     @JsonProperty("song_art_image_thumbnail_url") String thumbnail,
                     String url,
                     @JsonProperty("primary_artist") PrimaryArtist artist) {
}

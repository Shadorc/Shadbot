package com.shadorc.shadbot.api.image.wallhaven;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class WallhavenResponse {

    @JsonProperty("data")
    private List<Wallpaper> wallpapers;

    public List<Wallpaper> getWallpapers() {
        return Collections.unmodifiableList(this.wallpapers);
    }

}

package com.shadorc.shadbot.api.json.image.wallhaven;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WallhavenResponse(@JsonProperty("data") List<Wallpaper> wallpapers) {

}

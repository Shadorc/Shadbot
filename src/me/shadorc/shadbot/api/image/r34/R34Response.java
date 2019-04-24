package me.shadorc.shadbot.api.image.r34;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

public class R34Response {

    @Nullable
    @JsonProperty("posts")
    private R34Posts posts;

    @Nullable
    public R34Posts getPosts() {
        return this.posts;
    }

}

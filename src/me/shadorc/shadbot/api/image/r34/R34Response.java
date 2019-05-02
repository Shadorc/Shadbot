package me.shadorc.shadbot.api.image.r34;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class R34Response {

    @Nullable
    @JsonProperty("posts")
    private R34Posts posts;

    public Optional<R34Posts> getPosts() {
        return Optional.ofNullable(this.posts);
    }

}

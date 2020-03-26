package com.shadorc.shadbot.api.json.image.r34;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class R34Posts {

    @JsonProperty("post")
    @Nullable
    private List<R34Post> posts;

    public Optional<List<R34Post>> getPosts() {
        return Optional.ofNullable(this.posts).map(Collections::unmodifiableList);
    }

}

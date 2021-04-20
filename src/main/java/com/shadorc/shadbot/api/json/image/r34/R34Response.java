package com.shadorc.shadbot.api.json.image.r34;

import reactor.util.annotation.Nullable;

import java.util.Optional;

public record R34Response(@Nullable R34Posts posts) {

    public Optional<R34Posts> getPosts() {
        return Optional.ofNullable(this.posts);
    }

}

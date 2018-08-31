package me.shadorc.shadbot.api.image.r34;

import com.fasterxml.jackson.annotation.JsonProperty;

public class R34Response {

	@JsonProperty("posts")
	private R34Posts posts;

	public R34Posts getPosts() {
		return this.posts;
	}

	@Override
	public String toString() {
		return String.format("R34Response [posts=%s]", this.posts);
	}

}

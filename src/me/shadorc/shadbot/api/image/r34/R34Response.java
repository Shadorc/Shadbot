package me.shadorc.shadbot.api.image.r34;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class R34Response {

	@JsonProperty("posts")
	private List<R34Post> posts;
	@JsonProperty("count")
	private int count;

	public List<R34Post> getPosts() {
		return posts;
	}

	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "R34Response [posts=" + posts
				+ ", count=" + count
				+ "]";
	}

}

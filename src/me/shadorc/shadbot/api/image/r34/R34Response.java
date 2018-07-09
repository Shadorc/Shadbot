package me.shadorc.shadbot.api.image.r34;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class R34Response {

	@JsonProperty("posts")
	private List<PostReponse> posts;
	@JsonProperty("count")
	private int count;

	public List<PostReponse> getPosts() {
		return posts;
	}

	public int getCount() {
		return count;
	}

}

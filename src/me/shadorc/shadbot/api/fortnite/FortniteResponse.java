package me.shadorc.shadbot.api.fortnite;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FortniteResponse {

	@Nullable
	@JsonProperty("error")
	private String error;

}

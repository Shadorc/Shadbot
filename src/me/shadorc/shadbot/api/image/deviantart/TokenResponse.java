package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {

	@JsonProperty("access_token")
	private String accessToken;
	@JsonProperty("expires_in")
	private int expiresIn;

	public String getAccessToken() {
		return accessToken;
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	@Override
	public String toString() {
		return String.format("TokenResponse [accessToken=%s, expiresIn=%s]", accessToken, expiresIn);
	}

}

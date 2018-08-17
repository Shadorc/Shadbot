package me.shadorc.shadbot.api.twitter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class Twitter {

	private String bearerToken;

	public Twitter(String consumerKey, String consumerSecret) {
		try {
			this.bearerToken = this.requestBearerToken(consumerKey, consumerSecret);
		} catch (IOException err) {
			LogUtils.error(err, "An error occurred while getting Twitter bearer token.");
		}
	}

	/**
	 * Encodes the consumer key and secret to create the basic authorization key
	 * 
	 * @return Twitter Consumer API keys encoded to Base 64
	 * @throws UnsupportedEncodingException If the named encoding is not supported
	 */
	private String encodeKeys(String consumerKey, String consumerSecret) throws UnsupportedEncodingException {
		final String encodedConsumerKey = NetUtils.encode(consumerKey);
		final String encodedConsumerSecret = NetUtils.encode(consumerSecret);
		final String fullKey = String.format("%s:%s", encodedConsumerKey, encodedConsumerSecret);
		return Base64.getEncoder().encodeToString(fullKey.getBytes());
	}

	/**
	 * Constructs the request for requesting a bearer token and returns that token as a string
	 * 
	 * @return The bearer token as a string
	 * @throws IOException on error
	 */
	private String requestBearerToken(String consumerKey, String consumerSecret) throws IOException {
		Objects.requireNonNull(consumerKey);
		Objects.requireNonNull(consumerSecret);

		final String endPointUrl = "https://api.twitter.com/oauth2/token";
		Document doc = Jsoup.connect(endPointUrl)
				.ignoreContentType(true)
				.headers(Map.of("Host", "api.twitter.com", 
						"User-Agent", Config.USER_AGENT, 
						"Authorization", "Basic " + this.encodeKeys(consumerKey, consumerSecret), 
						"Content-Type", "application/x-www-form-urlencoded;charset=UTF-8",
						"Content-Length", "29"))
				.requestBody("grant_type=client_credentials")
				.post();

		JSONObject mainObj = new JSONObject(doc.text());
		return mainObj.getString("access_token");
	}

	/**
	 * Fetches the first tweet from a given user's timeline
	 * 
	 * @param screenName - user's screen name
	 * @return The first tweet from a given user's timeline
	 * @throws IOException on error
	 */
	public String getLastTweet(String screenName) throws IOException {
		if(bearerToken == null) {
			throw new IOException("Twitter bearer token is null.");
		}

		final String endPointUrl = "https://api.twitter.com/1.1/statuses/user_timeline.json?" 
				+ "screen_name=" + screenName 
				+ "&count=1";
		Document doc = Jsoup.connect(endPointUrl)
				.ignoreContentType(true)
				.headers(Map.of("Host", "api.twitter.com",
						"User-Agent", Config.USER_AGENT, 
						"Authorization", String.format("Bearer %s", bearerToken)))
				.get();

		JSONArray array = new JSONArray(doc.text());
		return array.getJSONObject(0).getString("text");
	}

}
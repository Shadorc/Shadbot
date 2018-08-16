package me.shadorc.shadbot.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;

public class TwitterUtils {

	/**
	 * Encodes the consumer key and secret to create the basic authorization key
	 * 
	 * @return Twitter Consumer API keys encoded to Base 64
	 * @throws UnsupportedEncodingException If the named encoding is not supported
	 */
	private static String encodeKeys() throws UnsupportedEncodingException {
		final String encodedConsumerKey = NetUtils.encode(APIKeys.get(APIKey.TWITTER_API_KEY));
		final String encodedConsumerSecret = NetUtils.encode(APIKeys.get(APIKey.TWITTER_API_SECRET));
		final String fullKey = encodedConsumerKey + ":" + encodedConsumerSecret;
		return Base64.getEncoder().encodeToString(fullKey.getBytes());
	}

	/**
	 * Constructs the request for requesting a bearer token and returns that token as a string
	 * 
	 * @return The bearer token as a string
	 * @throws IOException on error
	 */
	private static String requestBearerToken() throws IOException {
		final String endPointUrl = "https://api.twitter.com/oauth2/token";
		Document doc = Jsoup.connect(endPointUrl)
				.ignoreContentType(true)
				.headers(Map.of("Host", "api.twitter.com", 
						"User-Agent", Config.USER_AGENT, 
						"Authorization", "Basic " + encodeKeys(), 
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
	 * @param screen_name - user's screen name
	 * @return The first tweet from a given user's timeline
	 * @throws IOException on error
	 */
	public static String getLastTweet(String screen_name) throws IOException {
		final String endPointUrl = "https://api.twitter.com/1.1/statuses/user_timeline.json?" 
				+ "screen_name=" + screen_name 
				+ "&count=1";
		Document doc = Jsoup.connect(endPointUrl)
				.ignoreContentType(true)
				.headers(Map.of("Host", "api.twitter.com",
						"User-Agent", Config.USER_AGENT, 
						"Authorization", "Bearer " + requestBearerToken()))
				.get();

		JSONArray array = new JSONArray(doc.text());
		return array.getJSONObject(0).getString("text");
	}

}
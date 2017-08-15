package me.shadorc.discordbot.utils;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterUtils {

	private static Twitter twitter;

	public synchronized static void connection() {
		if(twitter == null) {
			twitter = TwitterFactory.getSingleton();
			twitter.setOAuthConsumer(Storage.getApiKey(ApiKeys.TWITTER_API_KEY), Storage.getApiKey(ApiKeys.TWITTER_API_SECRET));
			twitter.setOAuthAccessToken(new AccessToken(Storage.getApiKey(ApiKeys.TWITTER_TOKEN), Storage.getApiKey(ApiKeys.TWITTER_TOKEN_SECRET)));
		}
	}

	public static Twitter getInstance() {
		return twitter;
	}
}
package me.shadorc.discordbot.utility;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterUtils {

	private static Twitter twitter;

	public static void connection() {
		if(twitter == null) {
			twitter = TwitterFactory.getSingleton();
			twitter.setOAuthConsumer(Storage.get(ApiKeys.TWITTER_API_KEY), Storage.get(ApiKeys.TWITTER_API_SECRET));
			twitter.setOAuthAccessToken(new AccessToken(Storage.get(ApiKeys.TWITTER_TOKEN), Storage.get(ApiKeys.TWITTER_TOKEN_SECRET)));
		}
	}

	public static Twitter getInstance() {
		return twitter;
	}
}
package me.shadorc.discordbot.utils;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterUtils {

	private static Twitter twitter;

	public synchronized static void connection() {
		if(twitter == null) {
			twitter = TwitterFactory.getSingleton();
			twitter.setOAuthConsumer(Config.getAPIKey(APIKey.TWITTER_API_KEY), Config.getAPIKey(APIKey.TWITTER_API_SECRET));
			twitter.setOAuthAccessToken(new AccessToken(Config.getAPIKey(APIKey.TWITTER_TOKEN), Config.getAPIKey(APIKey.TWITTER_TOKEN_SECRET)));
		}
	}

	/**
	 * @return Twitter instance
	 */
	public static Twitter getInstance() {
		return twitter;
	}
}
package me.shadorc.shadbot.utils;

import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class TwitterUtils {

	private static final Twitter TWITTER = TwitterFactory.getSingleton();

	static {
		TWITTER.setOAuthConsumer(APIKeys.get(APIKey.TWITTER_API_KEY), APIKeys.get(APIKey.TWITTER_API_SECRET));
		TWITTER.setOAuthAccessToken(new AccessToken(APIKeys.get(APIKey.TWITTER_TOKEN), APIKeys.get(APIKey.TWITTER_TOKEN_SECRET)));
	}

	public static String getLastTweet(String user) throws TwitterException {
		return TWITTER.getUserTimeline(user).get(0).getText();
	}
}
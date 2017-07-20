package me.shadorc.discordbot;

import me.shadorc.discordbot.Storage.API_KEYS;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class Main {

	public static boolean DEBUG = false;

	private static IDiscordClient client;
	private static Twitter twitter;

	public static void main(String[] args) {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(Storage.get(API_KEYS.DISCORD_TOKEN));
		client = clientBuilder.login();

		EventDispatcher dispatcher = client.getDispatcher();
		dispatcher.registerListener(new AnnotationListener());
	}

	public static void twitterConnection() {
		if(twitter == null) {
			twitter = TwitterFactory.getSingleton();
			twitter.setOAuthConsumer(Storage.get(API_KEYS.TWITTER_API_KEY), Storage.get(API_KEYS.TWITTER_API_SECRET));
			twitter.setOAuthAccessToken(new AccessToken(Storage.get(API_KEYS.TWITTER_TOKEN), Storage.get(API_KEYS.TWITTER_TOKEN_SECRET)));
		}
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static Twitter getTwitter() {
		return twitter;
	}
}
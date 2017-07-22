package me.shadorc.discordbot;

import me.shadorc.discordbot.Storage.API_KEYS;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;

public class Main {

	public static boolean DEBUG = false;

	public static void main(String[] args) {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(Storage.get(API_KEYS.DISCORD_TOKEN));
		IDiscordClient client = clientBuilder.login();

		EventDispatcher dispatcher = client.getDispatcher();
		dispatcher.registerListener(new Listener());
	}

}
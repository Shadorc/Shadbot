package me.shadorc.discordbot;

import me.shadorc.discordbot.Storage.API_KEYS;
import me.shadorc.discordbot.music.GuildMusicManager;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;

public class Main {

	public static boolean DEBUG = false;

	public static void main(String[] args) {
		IDiscordClient client = new ClientBuilder()
				.withToken(Storage.get(API_KEYS.DISCORD_TOKEN))
				.login();

		client.getDispatcher().registerListener(new Listener());
		GuildMusicManager.init();
	}

}
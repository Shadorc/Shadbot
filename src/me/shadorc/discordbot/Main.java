package me.shadorc.discordbot;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.listener.ChannelListener;
import me.shadorc.discordbot.listener.EventListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;

public class Main {

	public static void main(String[] args) {
		IDiscordClient client = new ClientBuilder()
				.withToken(Storage.getApiKey(ApiKeys.DISCORD_TOKEN))
				.login();

		client.getDispatcher().registerListener(new EventListener());
		client.getDispatcher().registerListener(new ChannelListener());

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
		AudioSourceManagers.registerLocalSource(GuildMusicManager.PLAYER_MANAGER);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				client.logout();
				Log.info("Shadbot has been disconnected from all guilds.");
			}
		});
	}
}
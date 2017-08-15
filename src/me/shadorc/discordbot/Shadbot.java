package me.shadorc.discordbot;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.events.EventListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;

public class Shadbot {

	private static IDiscordClient client;

	public static void main(String[] args) {
		client = new ClientBuilder()
				.withToken(Storage.getApiKey(ApiKeys.DISCORD_TOKEN))
				.login();

		client.getDispatcher().registerListener(new EventListener());

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
		AudioSourceManagers.registerLocalSource(GuildMusicManager.PLAYER_MANAGER);
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static boolean hasPermission(IGuild guild, Permissions permission) {
		return client.getOurUser().getPermissionsForGuild(guild).contains(permission);
	}
}
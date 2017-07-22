package me.shadorc.discordbot;

import me.shadorc.discordbot.Storage.API_KEYS;
import me.shadorc.discordbot.music.GuildMusicManager;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class Main {

	public static boolean DEBUG = false;

	public static void main(String[] args) {
		IDiscordClient client = new ClientBuilder()
				.withToken(Storage.get(API_KEYS.DISCORD_TOKEN))
				.login();

		client.getDispatcher().registerListener(new Listener());
		GuildMusicManager.init();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for(IGuild guild : client.getGuilds()) {
					GuildMusicManager guildMusicManager = GuildMusicManager.getGuildAudioPlayer(guild);
					if(guildMusicManager != null) {
						guildMusicManager.getScheduler().stop();
						IVoiceChannel botVoiceChannel = client.getOurUser().getVoiceStateForGuild(guild).getChannel();
						if(botVoiceChannel != null) {
							botVoiceChannel.leave();
						}
					}
				}
			}
		});
	}

}
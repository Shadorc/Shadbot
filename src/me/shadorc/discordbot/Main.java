package me.shadorc.discordbot;

import java.util.Timer;
import java.util.TimerTask;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.events.EventListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;

public class Main {

	public static void main(String[] args) {
		IDiscordClient client = new ClientBuilder()
				.withToken(Storage.getApiKey(ApiKeys.DISCORD_TOKEN))
				.login();

		client.getDispatcher().registerListener(new EventListener());

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
		AudioSourceManagers.registerLocalSource(GuildMusicManager.PLAYER_MANAGER);

		// Update Shadbot stats every hour
		final int period = 1000 * 60 * 60;
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				NetUtils.postStats(client);
			}
		};
		timer.schedule(timerTask, period, period);
	}
}
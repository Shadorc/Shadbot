package me.shadorc.discordbot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.events.ReadyListener;
import me.shadorc.discordbot.events.ShardListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IUser;

public class Shadbot {

	private static final ExecutorService THREAD_POOL =
			Executors.newCachedThreadPool(Utils.getThreadFactoryNamed("Shadbot-ThreadPool-%d"));

	private static IDiscordClient client;
	private static IUser owner;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if(Shadbot.getClient() != null) {
					Shadbot.getClient().logout();
				}
				Scheduler.stop();
				Shadbot.getDefaultThreadPool().shutdownNow();
			}
		}));

		client = new ClientBuilder()
				.withToken(Config.get(APIKey.DISCORD_TOKEN))
				.withRecommendedShardCount()
				.setMaxMessageCacheCount(10)
				.setMaxReconnectAttempts(100)
				.login();

		LogUtils.info("Connecting to " + StringUtils.pluralOf(client.getShardCount(), "shard") + "...");

		client.getDispatcher().registerListener(Shadbot.getDefaultThreadPool(), new ReadyListener());
		client.getDispatcher().registerListener(Shadbot.getDefaultThreadPool(), new ShardListener());

		owner = client.getApplicationOwner();

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
	}

	public static ExecutorService getDefaultThreadPool() {
		return THREAD_POOL;
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static IUser getOwner() {
		return owner;
	}
}
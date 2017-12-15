package me.shadorc.discordbot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

	private static final ExecutorService EVENT_THREAD_POOL =
			new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() * 4,
					0, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(), Utils.getThreadFactoryNamed("Shadbot-EventThreadPool-%d"));
	private static final ExecutorService DEFAUT_THREAD_POOL =
			new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() * 4,
					0, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>(), Utils.getThreadFactoryNamed("Shadbot-DefaultThreadPool-%d"));

	private static IDiscordClient client;
	private static IUser owner;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				Scheduler.stop();
			}
		}));

		client = new ClientBuilder()
				.withToken(Config.get(APIKey.DISCORD_TOKEN))
				.withRecommendedShardCount()
				.setMaxMessageCacheCount(25)
				.setMaxReconnectAttempts(10)
				.login();

		LogUtils.info("Connecting to " + StringUtils.pluralOf(client.getShardCount(), "shard") + "...");

		client.getDispatcher().registerListener(Shadbot.getEventThreadPool(), new ReadyListener());
		client.getDispatcher().registerListener(Shadbot.getEventThreadPool(), new ShardListener());

		owner = client.getApplicationOwner();

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
	}

	public static ExecutorService getEventThreadPool() {
		return EVENT_THREAD_POOL;
	}

	public static ExecutorService getDefaultThreadPool() {
		return DEFAUT_THREAD_POOL;
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static IUser getOwner() {
		return owner;
	}
}
package me.shadorc.shadbot;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.listener.ReadyListener;
import me.shadorc.shadbot.listener.ShardListener;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.executor.ShadbotCachedExecutor;
import me.shadorc.shadbot.utils.executor.ShadbotScheduledExecutor;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.StatusType;

public class Shadbot {

	private static final ThreadPoolExecutor EVENT_THREAD_POOL = new ShadbotCachedExecutor("Shadbot-EventThreadPool-%d");
	private static final ScheduledThreadPoolExecutor DEFAULT_SCHEDULER = new ShadbotScheduledExecutor(2, "Shadbot-DefaultScheduler-%d");

	private static String version;
	private static IDiscordClient client;

	public static void main(String[] args) {
		try {
			Properties properties = new Properties();
			properties.load(Shadbot.class.getClassLoader().getResourceAsStream("project.properties"));
			version = properties.getProperty("version");
		} catch (IOException err) {
			LogUtils.errorf(err, "An error occurred while getting version.");
		}

		// Initialization
		if(!DataManager.init() || !CommandManager.init()) {
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				GuildMusicManager.stop();
				ShardManager.stop();
				DataManager.stop();
			}
		});

		client = new ClientBuilder()
				.withToken(APIKeys.get(APIKey.DISCORD_TOKEN))
				.withRecommendedShardCount()
				.setMaxMessageCacheCount(25)
				.setMaxReconnectAttempts(10)
				.setPresence(StatusType.IDLE)
				.build();

		LogUtils.infof("Connecting to %s...", StringUtils.pluralOf(client.getShardCount(), "shard"));

		client.getDispatcher().registerListeners(Shadbot.getEventThreadPool(), new ReadyListener(), new ShardListener());
		client.login();
	}

	public static String getVersion() {
		return version;
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static ThreadPoolExecutor getEventThreadPool() {
		return EVENT_THREAD_POOL;
	}

	public static ScheduledThreadPoolExecutor getScheduler() {
		return DEFAULT_SCHEDULER;
	}

}

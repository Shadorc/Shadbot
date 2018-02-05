package me.shadorc.shadbot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
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

	public static final String VERSION;

	private static final ThreadPoolExecutor EVENT_THREAD_POOL = new ShadbotCachedExecutor("EventThreadPool-%d");
	private static final ScheduledThreadPoolExecutor DEFAULT_SCHEDULER = new ShadbotScheduledExecutor(3, "DefaultScheduler-%d");

	private static IDiscordClient client;

	static {
		Properties properties = new Properties();
		try (InputStream inStream = Shadbot.class.getClassLoader().getResourceAsStream("project.properties")) {
			properties.load(inStream);
		} catch (IOException err) {
			LogUtils.error(err, "An error occurred while getting version.");
		}
		VERSION = properties.getProperty("version");
	}

	public static void main(String[] args) {
		Locale.setDefault(new Locale("en", "US"));

		// Initialization
		if(!DataManager.init() || !CommandManager.init()) {
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LogUtils.infof("Stopping guild music manager...");
				GuildMusicManager.stop();
				LogUtils.infof("Stopping shard manager...");
				ShardManager.stop();
				LogUtils.infof("Stopping data manager...");
				DataManager.stop();
			}
		});

		client = new ClientBuilder()
				.withToken(APIKeys.get(APIKey.DISCORD_TOKEN))
				.withRecommendedShardCount()
				.withPingTimeout(10)
				.setMaxReconnectAttempts(10)
				.setMaxMessageCacheCount(100)
				.setPresence(StatusType.IDLE)
				.build();

		LogUtils.infof("Connecting to %s...", StringUtils.pluralOf(client.getShardCount(), "shard"));

		client.getDispatcher().registerListeners(Shadbot.getEventThreadPool(), new ReadyListener(), new ShardListener());
		client.login();
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

package me.shadorc.shadbot;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shadorc.discordbot.utils.Utils;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.listener.ReadyListener;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.StatusType;

public class Shadbot {

	private static final ExecutorService EVENT_THREAD_POOL =
			Executors.newCachedThreadPool(Utils.getThreadFactoryNamed("Shadbot-EventThreadPool-%d"));

	private static String version;
	private static IDiscordClient client;

	public static void main(String[] args) {
		try {
			Properties properties = new Properties();
			properties.load(Shadbot.class.getClassLoader().getResourceAsStream("project.properties"));
			version = properties.getProperty("version");
		} catch (IOException err) {
			LogUtils.error("An error occurred while getting version.", err);
		}

		// Initialization
		if(!DataManager.init() || !CommandManager.init()) {
			return;
		}

		System.exit(0);

		client = new ClientBuilder()
				.withToken(APIKeys.get(APIKey.DISCORD_TOKEN))
				.withRecommendedShardCount()
				.setMaxMessageCacheCount(25)
				.setMaxReconnectAttempts(10)
				.setPresence(StatusType.IDLE) // TODO: Change to ONLINE when everything is ready
				.build();

		LogUtils.infof("Connecting to %s...", StringUtils.pluralOf(client.getShardCount(), "shard"));

		client.getDispatcher().registerListeners(Shadbot.getEventThreadPool(), new ReadyListener());
		// client.getDispatcher().registerListeners(Shadbot.getEventThreadPool(),
		// new ReadyListener(),
		// new ShardListener());

		// AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);

		client.login();
	}

	public static String getVersion() {
		return version;
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static ExecutorService getEventThreadPool() {
		return EVENT_THREAD_POOL;
	}

}

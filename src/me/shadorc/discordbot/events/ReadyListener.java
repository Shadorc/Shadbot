package me.shadorc.discordbot.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.events.music.VoiceChannelListener;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

@SuppressWarnings("ucd")
public class ReadyListener {

	private final ExecutorService guildsExecutor = Executors.newCachedThreadPool();
	private final ExecutorService channelsExecutor = Executors.newCachedThreadPool();
	private final ExecutorService messagesExecutor = Executors.newCachedThreadPool();
	private final ExecutorService voiceChannelsExecutor = Executors.newCachedThreadPool();

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		LogUtils.info("------------------- Shadbot is ready [Version:" + Config.VERSION.toString() + "] -------------------");

		Scheduler.start();

		Shadbot.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");
		Shadbot.getClient().getDispatcher().registerListener(guildsExecutor, new GuildListener());
		Shadbot.getClient().getDispatcher().registerListener(channelsExecutor, new ChannelListener());
		Shadbot.getClient().getDispatcher().registerListener(voiceChannelsExecutor, new VoiceChannelListener());
		Shadbot.getClient().getDispatcher().registerListener(messagesExecutor, new MessageListener());
	}
}

package me.shadorc.discordbot.events;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

@SuppressWarnings("ucd")
public class ReadyListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		LogUtils.info("------------------- Shadbot is ready [Version:" + Config.VERSION.toString() + "] -------------------");

		Scheduler.start();

		Shadbot.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");
		Shadbot.getClient().getDispatcher().registerListener(new GuildListener());
		Shadbot.getClient().getDispatcher().registerListener(new MessageListener());
		Shadbot.getClient().getDispatcher().registerListener(new ChannelListener());
		Shadbot.getClient().getDispatcher().registerListener(new VoiceChannelListener());
	}
}

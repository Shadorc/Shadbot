package me.shadorc.discordbot.events;

import java.util.Timer;
import java.util.TimerTask;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

@SuppressWarnings("ucd")
public class ReadyListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		LogUtils.info("------------------- Shadbot is ready [Version:" + Config.VERSION.toString() + "] -------------------");

		Shadbot.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");
		Shadbot.getClient().getDispatcher().registerListener(new EventListener());

		// Update Shadbot stats every 3 hours
		final int period = 1000 * 60 * 60 * 3;
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				NetUtils.postStats();
			}
		};
		timer.schedule(timerTask, 0, period);
	}

}

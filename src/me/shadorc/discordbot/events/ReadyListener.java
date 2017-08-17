package me.shadorc.discordbot.events;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

public class ReadyListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		LogUtils.info("------------------- Shadbot is ready [Version:" + Config.VERSION.toString() + "] -------------------");

		Shadbot.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");
		Shadbot.getClient().getDispatcher().registerListener(new EventListener());

		//TODO: Remove in next release
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(new File("data.json").toURI().toURL().openStream()));
			JSONArray namesArray = mainObj.names();
			for(int i = 0; i < namesArray.length(); i++) {
				try {
					Storage.saveSetting(
							Shadbot.getClient().getGuildByID(Long.parseLong(namesArray.getString(i))),
							Setting.VOLUME,
							Config.DEFAULT_VOLUME);
				} catch (NullPointerException e) {
					Storage.removeGuild(namesArray.getString(i));
					System.err.println(namesArray.get(i) + " is null, removing.");
				}
			}
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			System.exit(0);
		}

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

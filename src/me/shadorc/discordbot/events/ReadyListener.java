package me.shadorc.discordbot.events;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

@SuppressWarnings("ucd")
public class ReadyListener {

	@EventSubscriber
	public void onReadyEvent(ReadyEvent event) {
		LogUtils.info("------------------- Shadbot is ready [Version:" + Config.VERSION.toString() + "] -------------------");

		Shadbot.getClient().changePlayingText(Config.DEFAULT_PREFIX + "help");
		Shadbot.getClient().getDispatcher().registerListener(new GuildListener());
		Shadbot.getClient().getDispatcher().registerListener(new MessageListener());
		Shadbot.getClient().getDispatcher().registerListener(new ChannelListener());
		Shadbot.getClient().getDispatcher().registerListener(new VoiceChannelListener());

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

		this.convertData();
	}

	// TODO: Remove in next release
	@SuppressWarnings("PMD.EmptyCatchBlock")
	private void convertData() {
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(new File("data.json").toURI().toURL().openStream()));
			JSONObject cleanedMainObj = new JSONObject();

			for(int guildIndex = 0; guildIndex < mainObj.names().length(); guildIndex++) {
				String guildID = mainObj.names().getString(guildIndex);
				if(Shadbot.getClient().getGuildByID(Long.parseLong(guildID)) == null) {
					LogUtils.info("Non existent guild detected, deleting it. (ID: " + guildID + ")");
				} else {
					cleanedMainObj.put(guildID, mainObj.getJSONObject(guildID));
				}
			}

			for(int guildIndex = 0; guildIndex < cleanedMainObj.names().length(); guildIndex++) {
				String guildID = cleanedMainObj.names().getString(guildIndex);
				JSONObject guildObj = (JSONObject) cleanedMainObj.getJSONObject(guildID);

				if(guildObj.has(Setting.ALLOWED_CHANNELS.toString())) {
					try {
						List<String> allowedChannels = Utils.convertToStringList(guildObj.getJSONArray(Setting.ALLOWED_CHANNELS.toString()));
						JSONArray convertedAllowedChannels = new JSONArray();

						for(String channelID : allowedChannels) {
							if(Shadbot.getClient().getChannelByID(Long.parseLong(channelID)) == null) {
								LogUtils.info("Non existent channel detected, deleting it. (ID: " + channelID + ")");
							} else {
								LogUtils.info("Converting channel ID to Long. (ID: " + channelID + ")");
								convertedAllowedChannels.put(Long.parseLong(channelID));
							}
						}

						guildObj.put(Setting.ALLOWED_CHANNELS.toString(), convertedAllowedChannels);
					} catch (JSONException ignored) {
					}

				} else {
					LogUtils.info("Missing allowed_channels value, initializing it. (Guild ID: " + guildID + ")");
					guildObj.put(Setting.ALLOWED_CHANNELS.toString(), new JSONArray());
				}

				for(int objIndex = 0; objIndex < guildObj.names().length(); objIndex++) {
					Object obj = guildObj.get((String) guildObj.names().get(objIndex));

					// User
					if(obj instanceof JSONObject) {
						JSONObject userObj = (JSONObject) obj;
						long coins = userObj.getLong("coins");
						if(Math.abs(coins) > Integer.MAX_VALUE) {
							LogUtils.info("Wrong coins value detected, modifying it. (Value: " + coins + ")");
							userObj.put("coins", Integer.MAX_VALUE);
						}
					}
				}

				cleanedMainObj.put(guildID, guildObj);
			}

			FileWriter writer = null;
			try {
				writer = new FileWriter(new File("data.json"));
				writer.write(cleanedMainObj.toString(2));
				writer.flush();

			} catch (IOException err) {
				LogUtils.error("Error while saving setting.", err);

			} finally {
				IOUtils.closeQuietly(writer);
			}

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while converting data.", err);
		}
	}
}

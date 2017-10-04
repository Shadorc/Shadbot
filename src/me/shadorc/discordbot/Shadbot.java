package me.shadorc.discordbot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.events.ReadyListener;
import me.shadorc.discordbot.events.ShardListener;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IUser;

public class Shadbot {

	private static IDiscordClient client;
	private static IUser owner;

	public static void main(String[] args) {
		convertData();
		cleanData();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				Scheduler.forceAndWaitExecution();
			}
		}));

		client = new ClientBuilder()
				.withToken(Config.get(APIKey.DISCORD_TOKEN))
				.setMaxMessageCacheCount(250)
				.setMaxReconnectAttempts(100)
				.login();

		client.getDispatcher().registerListener(new ReadyListener());
		client.getDispatcher().registerListener(new ShardListener());

		owner = client.getApplicationOwner();

		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
	}

	public static IDiscordClient getClient() {
		return client;
	}

	public static IUser getOwner() {
		return owner;
	}

	// TODO: Remove in next release
	@SuppressWarnings("PMD.AvoidPrintStackTrace")
	private static void convertData() {
		File dataFile = new File("data.json");
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(dataFile.toURI().toURL().openStream()));
			JSONObject newMainObj = new JSONObject();

			for(Object key : mainObj.keySet()) {
				String guildID = key.toString();
				JSONObject newGuildObj = new JSONObject();
				newGuildObj.put("users", new JSONArray());
				newGuildObj.put("settings", new JSONObject());
				JSONObject guildObj = mainObj.getJSONObject(guildID);
				for(Object obj : guildObj.keySet()) {
					// User
					if(StringUtils.isPositiveLong(obj.toString())) {
						newGuildObj.getJSONArray("users").put(new JSONObject()
								.put("userID", obj)
								.put("coins", guildObj.getJSONObject(obj.toString()).getInt("coins")));
					}
					// Setting
					else {
						newGuildObj.getJSONObject("settings").put(obj.toString(), guildObj.get(obj.toString()));
					}
				}
				newMainObj.put(guildID, newGuildObj);
			}

			FileWriter writer = null;
			try {
				writer = new FileWriter(dataFile);
				writer.write(newMainObj.toString(Config.INDENT_FACTOR));
				writer.flush();
			} finally {
				IOUtils.closeQuietly(writer);
			}
		} catch (JSONException | IOException err) {
			err.printStackTrace();
		}
	}

	// TODO: Remove in next release
	@SuppressWarnings({ "PMD.AvoidPrintStackTrace", "PMD.SystemPrintln" })
	private static void cleanData() {
		File dataFile = new File("data.json");
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(dataFile.toURI().toURL().openStream()));

			List<String> toDeleteList = new ArrayList<>();
			for(Object key : mainObj.keySet()) {
				JSONObject guildObj = mainObj.getJSONObject(key.toString());
				JSONObject settingsObj = guildObj.getJSONObject("settings");
				JSONArray usersArray = guildObj.getJSONArray("users");
				if(usersArray.length() != 0 || settingsObj.keySet().size() != 3) {
					continue;
				}

				boolean toDelete = true;
				for(Object settingKey : settingsObj.keySet()) {
					Object obj = settingsObj.get(settingKey.toString());
					if(obj instanceof String && !obj.equals(Config.DEFAULT_PREFIX)
							|| obj instanceof JSONArray && ((JSONArray) obj).length() > 0
							|| obj instanceof Integer && ((int) obj) != Config.DEFAULT_VOLUME) {
						toDelete = false;
						break;
					}
				}

				if(toDelete) {
					toDeleteList.add(key.toString());
				}
			}

			for(String guildID : toDeleteList) {
				System.err.println("Deleting guild: " + guildID);
				mainObj.remove(guildID);
			}

			FileWriter writer = null;
			try {
				writer = new FileWriter(dataFile);
				writer.write(mainObj.toString(Config.INDENT_FACTOR));
				writer.flush();

			} finally {
				IOUtils.closeQuietly(writer);
			}
		} catch (JSONException | IOException err) {
			err.printStackTrace();
		}
	}
}
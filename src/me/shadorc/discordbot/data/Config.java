package me.shadorc.discordbot.data;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.Version;
import me.shadorc.discordbot.utils.LogUtils;

public class Config {

	public static final Version VERSION = new Version(1, 6, 2, false);

	public static final long DEBUG_CHANNEL_ID = 342074301840752640L;
	public static final long SUGGEST_CHANNEL_ID = 345898633805430785L;
	public static final long LOGS_CHANNEL_ID = 346311941829951489L;

	public static final Color BOT_COLOR = new Color(170, 196, 222);

	public static final String DEFAULT_PREFIX = "/";
	public static final int DEFAULT_VOLUME = 10;

	private static final File API_KEYS_FILE = new File("api_keys.json");

	public enum APIKey {
		GIPHY_API_KEY,
		DTC_API_KEY,
		DISCORD_TOKEN,
		TWITTER_API_KEY,
		TWITTER_API_SECRET,
		TWITTER_TOKEN,
		TWITTER_TOKEN_SECRET,
		STEAM_API_KEY,
		OPENWEATHERMAP_API_KEY,
		DEVIANTART_CLIENT_ID,
		DEVIANTART_API_SECRET,
		BOTS_DISCORD_PW_TOKEN,
		DISCORD_BOTS_ORG_TOKEN,
		BLIZZARD_API_KEY,
		CLEVERBOT_API_KEY;
	}

	public static synchronized String getAPIKey(APIKey key) {
		try {
			JSONObject mainObj = new JSONObject(new JSONTokener(API_KEYS_FILE.toURI().toURL().openStream()));
			return mainObj.optString(key.toString());

		} catch (IOException err) {
			LogUtils.error("Error while reading API keys file.", err);
		}

		return null;
	}
}
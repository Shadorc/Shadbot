package me.shadorc.discordbot.data;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.Version;
import me.shadorc.discordbot.utils.LogUtils;

public class Config {

	public static final Version VERSION = new Version(1, 9, 11);

	public static final long SUGGEST_CHANNEL_ID = 345898633805430785L;
	public static final long LOGS_CHANNEL_ID = 346311941829951489L;
	public static final long PM_CHANNEL_ID = 386908035949527042L;

	public static final int INDENT_FACTOR = 2;

	public static final int MAX_PLAYLIST_SIZE = 200;
	public static final int MAX_COINS = Integer.MAX_VALUE;

	public static final int DEFAULT_TIMEOUT = 5000;

	public static final int MESSAGE_RETRY_INTERVAL = 3;
	public static final int MESSAGE_RETRY_COUNT = 2;

	public static final Color BOT_COLOR = new Color(170, 196, 222);

	public static final String DEFAULT_PREFIX = "/";
	public static final int DEFAULT_VOLUME = 10;

	private static final File API_KEYS_FILE = new File("api_keys.json");
	private static final Map<APIKey, String> API_KEYS_MAP = new HashMap<>();

	static {
		if(!API_KEYS_FILE.exists()) {
			LogUtils.LOGGER.error("API keys file not found ! Exiting.");
			System.exit(1);
		}

		try (InputStream stream = API_KEYS_FILE.toURI().toURL().openStream()) {
			JSONObject mainObj = new JSONObject(new JSONTokener(stream));
			for(APIKey key : APIKey.values()) {
				API_KEYS_MAP.put(key, mainObj.getString(key.toString()));
			}

		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("Error while reading API keys file. Exiting.", err);
			System.exit(1);
		}
	}

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
		WALLHAVEN_LOGIN,
		WALLHAVEN_PASSWORD;
	}

	public static String get(APIKey key) {
		return API_KEYS_MAP.get(key);
	}
}
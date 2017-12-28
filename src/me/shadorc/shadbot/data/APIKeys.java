package me.shadorc.shadbot.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

public class APIKeys {

	private static final Properties KEYS_PROPERTIES = new Properties();
	private static final File API_KEYS_FILE = new File("api_keys.properties");

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

	@DataInit
	public static void init() throws MalformedURLException, IOException {
		try (FileReader reader = new FileReader(API_KEYS_FILE)) {
			KEYS_PROPERTIES.load(reader);
		}

		for(APIKey key : APIKey.values()) {
			if(APIKeys.get(key) == null) {
				throw new ExceptionInInitializerError(String.format("%s not found.", key.toString()));
			}
		}
	}

	public static String get(APIKey key) {
		return KEYS_PROPERTIES.getProperty(key.toString());
	}

}

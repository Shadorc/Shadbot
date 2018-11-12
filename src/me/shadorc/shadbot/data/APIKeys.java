package me.shadorc.shadbot.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.shadbot.ExitCode;
import me.shadorc.shadbot.data.annotation.DataInit;

public class APIKeys {

	private static final Logger LOGGER = LoggerFactory.getLogger(APIKeys.class);

	private static final Properties KEYS_PROPERTIES = new Properties();
	private static final File API_KEYS_FILE = new File("api_keys.properties");

	public enum APIKey {
		GIPHY_API_KEY,
		DTC_API_KEY,
		DISCORD_TOKEN,
		TWITTER_API_KEY,
		TWITTER_API_SECRET,
		STEAM_API_KEY,
		OPENWEATHERMAP_API_KEY,
		DEVIANTART_CLIENT_ID,
		DEVIANTART_API_SECRET,
		BOTS_DISCORD_PW_TOKEN,
		DISCORD_BOTS_ORG_TOKEN,
		BLIZZARD_CLIENT_ID,
		BLIZZARD_CLIENT_SECRET,
		WALLHAVEN_LOGIN,
		WALLHAVEN_PASSWORD,
		FORTNITE_API_KEY;
	}

	@DataInit
	public static void init() throws MalformedURLException, IOException {
		if(!API_KEYS_FILE.exists()) {
			LOGGER.error("API keys file is missing. Exiting.");
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		try (FileReader reader = new FileReader(API_KEYS_FILE)) {
			KEYS_PROPERTIES.load(reader);
		}

		// Check if all API keys are present
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

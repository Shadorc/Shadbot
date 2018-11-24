package me.shadorc.shadbot.data.apikey;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class APIKeys {

	private static final Properties KEYS_PROPERTIES = new Properties();
	private static final File API_KEYS_FILE = new File("api_keys.properties");

	public APIKeys() throws IOException {
		if(!API_KEYS_FILE.exists()) {
			throw new RuntimeException("API keys file is missing. Exiting.");
		}

		try (FileReader reader = new FileReader(API_KEYS_FILE)) {
			KEYS_PROPERTIES.load(reader);
		}

		// Check if all API keys are present
		for(APIKey key : APIKey.values()) {
			if(APIKeys.get(key) == null) {
				throw new RuntimeException(String.format("%s not found.", key.toString()));
			}
		}
	}

	public static String get(APIKey key) {
		return KEYS_PROPERTIES.getProperty(key.toString());
	}

}

package me.shadorc.shadbot.data.apikey;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class APIKeys {

	private static final Properties KEYS_PROPERTIES = new Properties();
	private static final File API_KEYS_FILE = new File("api_keys.properties");

	public APIKeys() throws IOException {
		if(!API_KEYS_FILE.exists()) {
			throw new RuntimeException("API keys file is missing. Exiting.");
		}

		try (BufferedReader reader = Files.newBufferedReader(API_KEYS_FILE.toPath())) {
			KEYS_PROPERTIES.load(reader);
		}

		// Check if all API keys are present
		for(APIKey key : APIKey.values()) {
			if(this.get(key) == null) {
				throw new RuntimeException(String.format("%s not found.", key.toString()));
			}
		}
	}

	public String get(APIKey key) {
		return KEYS_PROPERTIES.getProperty(key.toString());
	}

}

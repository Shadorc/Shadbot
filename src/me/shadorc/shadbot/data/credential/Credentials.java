package me.shadorc.shadbot.data.credential;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class Credentials {

	private static final Properties CREDENTIALS_PROPERTIES = new Properties();
	private static final File CREDENTIALS_FILE = new File("credentials.properties");

	static {
		if(!CREDENTIALS_FILE.exists()) {
			throw new RuntimeException(String.format("%s file is missing.", CREDENTIALS_FILE.getName()));
		}

		try (BufferedReader reader = Files.newBufferedReader(CREDENTIALS_FILE.toPath())) {
			CREDENTIALS_PROPERTIES.load(reader);
		} catch (IOException e) {
			throw new RuntimeException(String.format("An error occurred while loading %s file.", CREDENTIALS_FILE.getName()));
		}

		// Check if all API keys are present
		for(Credential key : Credential.values()) {
			if(Credentials.get(key) == null) {
				throw new RuntimeException(String.format("%s not found.", key.toString()));
			}
		}
	}

	public static String get(Credential key) {
		return CREDENTIALS_PROPERTIES.getProperty(key.toString());
	}

}

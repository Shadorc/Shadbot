package me.shadorc.shadbot.data.credential;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class Credentials {

	private static final Properties CREDENTIALES_PROPERTIES = new Properties();
	private static final File CREDENTIALS_FILE = new File("credentials.properties");

	public Credentials() throws IOException {
		if(!CREDENTIALS_FILE.exists()) {
			throw new RuntimeException("API keys file is missing. Exiting.");
		}

		try (BufferedReader reader = Files.newBufferedReader(CREDENTIALS_FILE.toPath())) {
			CREDENTIALES_PROPERTIES.load(reader);
		}

		// Check if all API keys are present
		for(Credential key : Credential.values()) {
			if(this.get(key) == null) {
				throw new RuntimeException(String.format("%s not found.", key.toString()));
			}
		}
	}

	public String get(Credential key) {
		return CREDENTIALES_PROPERTIES.getProperty(key.toString());
	}

}

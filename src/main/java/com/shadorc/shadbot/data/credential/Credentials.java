package com.shadorc.shadbot.data.credential;

import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public final class Credentials {

    private static final Logger LOGGER = Loggers.getLogger(Credentials.class);
    private static final Properties CREDENTIALS_PROPERTIES = new Properties();
    private static final File CREDENTIALS_FILE = new File("credentials.properties");

    static {
        if (!CREDENTIALS_FILE.exists()) {
            throw new RuntimeException(String.format("%s file is missing.", CREDENTIALS_FILE.getName()));
        }

        try (final BufferedReader reader = Files.newBufferedReader(CREDENTIALS_FILE.toPath())) {
            CREDENTIALS_PROPERTIES.load(reader);
        } catch (final IOException e) {
            throw new RuntimeException(String.format("An error occurred while loading %s file.", CREDENTIALS_FILE.getName()));
        }

        // Check if all API keys are present
        for (final Credential key : Credential.values()) {
            if (Credentials.get(key) == null) {
                LOGGER.warn("Credential {} not found, the associated command / service may not work properly.", key);
            }
        }
    }

    public static String get(Credential key) {
        return CREDENTIALS_PROPERTIES.getProperty(key.toString());
    }

}

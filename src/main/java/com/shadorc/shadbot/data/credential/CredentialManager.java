package com.shadorc.shadbot.data.credential;

import com.shadorc.shadbot.utils.LogUtil;
import reactor.util.Logger;
import reactor.util.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

public class CredentialManager {

    private static final Logger LOGGER = LogUtil.getLogger(CredentialManager.class);

    private static CredentialManager instance;

    static {
        CredentialManager.instance = new CredentialManager();
    }

    private final Properties properties;

    private CredentialManager() {
        final File file = new File("credentials.properties");
        if (!file.exists()) {
            throw new RuntimeException("%s file is missing.".formatted(file.getName()));
        }

        this.properties = new Properties();
        try (final BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            this.properties.load(reader);
        } catch (final IOException err) {
            throw new RuntimeException("An error occurred while loading %s file.".formatted(file.getName()));
        }

        // Check if all API keys are present
        for (final Credential credential : Credential.values()) {
            if (this.get(credential) == null) {
                LOGGER.warn("Credential {} not found, the associated command/service may not work properly", credential);
            }
        }
    }

    @Nullable
    public String get(Credential key) {
        Objects.requireNonNull(key);
        final String property = this.properties.getProperty(key.toString());
        if (property == null || property.isBlank()) {
            return null;
        }
        return property;
    }

    public static CredentialManager getInstance() {
        return CredentialManager.instance;
    }

}

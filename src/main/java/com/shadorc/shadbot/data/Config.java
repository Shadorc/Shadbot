package com.shadorc.shadbot.data;

import com.shadorc.shadbot.utils.ExitCode;
import discord4j.core.object.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Config {

    private static final Logger LOGGER = Loggers.getLogger(Config.class);

    private static final Properties PROPERTIES = Config.getProperties();

    public static final String VERSION = PROPERTIES.getProperty("version");
    public static final boolean IS_SNAPSHOT = VERSION.endsWith("SNAPSHOT");
    public static final List<Snowflake> ADDITIONAL_OWNERS = Collections.unmodifiableList(Config.parseAdditionalOwners());
    public static final String GITHUB_URL = PROPERTIES.getProperty("url.github");
    public static final String PATREON_URL = PROPERTIES.getProperty("url.patreon");
    public static final String SUPPORT_SERVER_URL = PROPERTIES.getProperty("url.support.server");
    public static final String USER_AGENT = String.format("Shadbot/%s/D4J-DiscordBot (%s)", VERSION, GITHUB_URL);

    public static final Integer PREMIUM_MAX_VOLUME = Integer.parseInt(PROPERTIES.getProperty("premium.max.volume"));
    public static final Integer DEFAULT_MAX_VOLUME = Integer.parseInt(PROPERTIES.getProperty("default.max.volume"));
    public static final String DEFAULT_PREFIX = PROPERTIES.getProperty("default.prefix");
    public static final int DEFAULT_VOLUME = Integer.parseInt(PROPERTIES.getProperty("default.volume"));
    public static final int DEFAULT_PLAYLIST_SIZE = Integer.parseInt(PROPERTIES.getProperty("default.playlist.size"));
    public static final int DEFAULT_SAVED_PLAYLIST_SIZE = Integer.parseInt(PROPERTIES.getProperty("default.saved.playlist.size"));
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(Long.parseLong(PROPERTIES.getProperty("default.timeout")));
    public static final String DEFAULT_COMMAND_DELIMITER = PROPERTIES.getProperty("default.command.delimiter");

    public static final long MAX_COINS = Long.parseLong(PROPERTIES.getProperty("max.coins"));

    public static final int MUSIC_SEARCHES = Integer.parseInt(PROPERTIES.getProperty("music.searches"));
    public static final int MUSIC_CHOICE_DURATION = Integer.parseInt(PROPERTIES.getProperty("music.choice.duration"));

    public static final Snowflake LOGS_CHANNEL_ID = Snowflake.of(PROPERTIES.getProperty("id.channel.log"));
    public static final Color BOT_COLOR = Color.decode(PROPERTIES.getProperty("color"));

    private static Properties getProperties() {
        final Properties properties = new Properties();
        try (final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("project.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (final IOException err) {
            LOGGER.error("An error occurred while loading configuration file. Exiting.", err);
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }
        return properties;
    }

    private static List<Snowflake> parseAdditionalOwners() {
        final String property = PROPERTIES.getProperty("additional.owners");
        final String[] owners = property.split(",");
        return Arrays.stream(owners)
                .map(Snowflake::of)
                .collect(Collectors.toList());
    }

}

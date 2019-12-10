package com.shadorc.shadbot.data;

import com.shadorc.shadbot.utils.ExitCode;
import discord4j.core.object.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public final class Config {

    private static final Logger LOGGER = Loggers.getLogger(Config.class);

    private static final Properties PROPERTIES = Config.getProperties();

    public static final String VERSION = PROPERTIES.getProperty("version");
    public static final boolean IS_SNAPSHOT = VERSION.endsWith("SNAPSHOT");
    public static final String GITHUB_URL = PROPERTIES.getProperty("github.url");
    public static final String PATREON_URL = PROPERTIES.getProperty("patreon.url");
    public static final String SUPPORT_SERVER_URL = PROPERTIES.getProperty("support.server.url");
    public static final String USER_AGENT = String.format("Shadbot/%s/D4J-DiscordBot (%s)", VERSION, GITHUB_URL);

    public static final String DEFAULT_PREFIX = PROPERTIES.getProperty("default.prefix");
    public static final String COMMAND_DELIMITER = PROPERTIES.getProperty("command.delimiter");

    public static final int DEFAULT_VOLUME = Integer.parseInt(PROPERTIES.getProperty("default.volume"));
    public static final int VOLUME_MAX_PREMIUM = Integer.parseInt(PROPERTIES.getProperty("volume.max.premium"));
    public static final int VOLUME_MAX = Integer.parseInt(PROPERTIES.getProperty("volume.max"));
    public static final int PLAYLIST_SIZE = Integer.parseInt(PROPERTIES.getProperty("playlist.size"));
    public static final int SAVED_PLAYLIST_SIZE = Integer.parseInt(PROPERTIES.getProperty("saved.playlist.size"));
    public static final int MUSIC_SEARCHES = Integer.parseInt(PROPERTIES.getProperty("music.searches"));
    public static final int MUSIC_CHOICE_DURATION = Integer.parseInt(PROPERTIES.getProperty("music.choice.duration"));

    public static final Snowflake LOGS_CHANNEL_ID = Snowflake.of(PROPERTIES.getProperty("log.channel.id"));
    public static final Color BOT_COLOR = Color.decode(PROPERTIES.getProperty("embed.color"));
    public static final Duration TIMEOUT = Duration.ofMillis(Long.parseLong(PROPERTIES.getProperty("timeout")));
    public static final int RELIC_DURATION = Integer.parseInt(PROPERTIES.getProperty("relic.duration"));
    public static final long MAX_COINS = Long.parseLong(PROPERTIES.getProperty("coins.max"));
    public static final String DATABASE_NAME = PROPERTIES.getProperty("database.name");
    public static final String IPV6_BLOCK = PROPERTIES.getProperty("ipv6.block");

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

}

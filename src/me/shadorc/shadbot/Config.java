package me.shadorc.shadbot;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.annotation.DataInit;

public class Config {

	public static Properties properties;

	public static final String VERSION = properties.getProperty("version");
	public static final String GITHUB_URL = properties.getProperty("github.url");
	public static final String PATREON_URL = properties.getProperty("patreon.url");
	public static final String SUPPORT_SERVER_URL = properties.getProperty("support.server.url");
	public static final String USER_AGENT = String.format("Shadbot/%s/D4J-DiscordBot (%s)", VERSION, GITHUB_URL);

	public static final Snowflake LOGS_CHANNEL_ID = Snowflake.of(properties.getProperty("logs.channel.id"));

	public static final String DEFAULT_PREFIX = properties.getProperty("default.prefix");
	public static final int DEFAULT_VOLUME = Integer.parseInt(properties.getProperty("default.volume"));
	public static final int DEFAULT_PLAYLIST_SIZE = Integer.parseInt(properties.getProperty("default.playlist.size"));

	public static final int MAX_COINS = Integer.parseInt(properties.getProperty("default.max.coins"));

	public static final int JSON_INDENT_FACTOR = Integer.parseInt(properties.getProperty("json.indent.factor"));
	public static final int DEFAULT_TIMEOUT = Integer.parseInt(properties.getProperty("default.timeout"));

	public static final Color BOT_COLOR = Color.decode(properties.getProperty("color"));

	@DataInit
	public static void initProperties() throws IOException {
		properties = new Properties();
		try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("project.properties")) {
			properties.load(inputStream);
		}
	}

}

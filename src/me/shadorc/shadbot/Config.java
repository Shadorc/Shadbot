package me.shadorc.shadbot;

import java.awt.Color;

public class Config {

	public static final long LOGS_CHANNEL_ID = 346311941829951489L;

	public static final String GITHUB_URL = "https://github.com/Shadorc/Shadbot";
	public static final String PATREON_URL = "www.patreon.com/shadbot";
	public static final String SUPPORT_SERVER = "https://discord.gg/CKnV4ff";

	public static final int JSON_INDENT_FACTOR = 2;

	public static final int MAX_PLAYLIST_SIZE = 200;
	public static final int MAX_COINS = Integer.MAX_VALUE;

	public static final int DEFAULT_TIMEOUT = 20_000;
	public static final String USER_AGENT = String.format("Shadbot/%s/D4J-DiscordBot (%s)", Shadbot.VERSION, GITHUB_URL);

	public static final Color BOT_COLOR = new Color(170, 196, 222);

	public static final String DEFAULT_PREFIX = "/";
	public static final int DEFAULT_VOLUME = 10;

}

package me.shadorc.shadbot.music;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.core.object.util.Snowflake;
import discord4j.voice.VoiceConnection;

public class GuildVoiceManager {

	private static final Map<Snowflake, VoiceConnection> GUILD_VOICE_MAP = new ConcurrentHashMap<>();

	public static boolean contains(Snowflake guildId) {
		return GUILD_VOICE_MAP.containsKey(guildId);
	}

	public static VoiceConnection put(Snowflake guildId, VoiceConnection connection) {
		return GUILD_VOICE_MAP.put(guildId, connection);
	}

	public static VoiceConnection remove(Snowflake guildId) {
		return GUILD_VOICE_MAP.remove(guildId);
	}

}

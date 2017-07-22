package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import sx.blah.discord.handle.obj.IGuild;

public class GuildsMusicManager {

	public static final Map<IGuild, MusicManager> GUILDS = new HashMap <> ();

	public static void addMusicManager(IGuild guild) {
		if(!GUILDS.containsKey(guild)) {
			GUILDS.put(guild, new MusicManager(guild));
		}
	}

	public static void delMusicManager(IGuild guild) {
		if(GUILDS.containsKey(guild)) {
			GUILDS.remove(guild);
		}
	}

	public static MusicManager getMusicManager(IGuild guild) {
		return GUILDS.get(guild);
	}
}

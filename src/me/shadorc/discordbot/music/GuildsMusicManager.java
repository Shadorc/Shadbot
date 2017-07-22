package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import sx.blah.discord.handle.obj.IGuild;

public class GuildsMusicManager {

	public static final Map<IGuild, MusicManager> GUILDS = new HashMap <> ();

	public static void addMusicPlayer(IGuild guild) {
		if(!GUILDS.containsKey(guild)) {
			GUILDS.put(guild, new MusicManager(guild));
		}
	}

	public static void delMusicPlayer(IGuild guild) {
		if(GUILDS.containsKey(guild)) {
			GUILDS.remove(guild);
		}
	}

	public static MusicManager getMusicPlayer(IGuild guild) {
		return GUILDS.get(guild);
	}
}

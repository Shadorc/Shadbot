package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import sx.blah.discord.handle.obj.IGuild;

public class MusicManager {

	public static final Map<IGuild, Music> GUILDS = new HashMap <> ();

	public static void addMusicPlayer(IGuild guild) {
		if(!GUILDS.containsKey(guild)) {
			GUILDS.put(guild, new Music(guild));
		}
	}

	public static void delMusicPlayer(IGuild guild) {
		if(GUILDS.containsKey(guild)) {
			GUILDS.remove(guild);
		}
	}

	public static Music getMusicPlayer(IGuild guild) {
		return GUILDS.get(guild);
	}
}

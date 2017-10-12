package me.shadorc.discordbot.utils;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.command.Emoji;

public class TextUtils {

	public static final String PLAYLIST_LIMIT_REACHED =
			Emoji.GREY_EXCLAMATION + " You've reached the maximum number of tracks in the playlist (Max: " + Config.MAX_PLAYLIST_SIZE + ").";

}

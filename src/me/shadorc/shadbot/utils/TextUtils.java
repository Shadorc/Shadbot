package me.shadorc.shadbot.utils;

import org.jsoup.Jsoup;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

import discord4j.core.object.util.Permission;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.TextFile;
import me.shadorc.shadbot.utils.object.Emoji;

public class TextUtils {

	public static final String NOT_ENOUGH_COINS =
			"You don't have enough coins. You can get some by playing **RPS**, **Hangman** or **Trivia**.";

	public static final String PLAYLIST_LIMIT_REACHED =
			String.format(Emoji.WARNING + " You've reached the maximum number (%d) of tracks in a playlist. "
					+ "You can remove this limit by contributing to Shadbot. More info on **%s**", Config.DEFAULT_PLAYLIST_SIZE, Config.PATREON_URL);

	public static final TextFile SPAMS = new TextFile("texts/spam.txt");

	public static final String[] TIP_MESSAGES = { String.format("Check %slottery", Config.DEFAULT_PREFIX),
			String.format("Add a music first using %splayfirst", Config.DEFAULT_PREFIX),
			String.format("Help me keep Shadbot alive ! %s", Config.PATREON_URL),
			String.format("Support server: %s", Config.SUPPORT_SERVER_URL) };

	/**
	 * @param err - the exception containing the error message to clean
	 * @return A cleaned version of the error message, without HTML tags and YouTube links
	 */
	public static String cleanLavaplayerErr(FriendlyException err) {
		return Jsoup.parse(StringUtils.remove(err.getMessage(), "Watch on YouTube")).text().trim();
	}

	public static String missingPermission(String username, Permission permission) {
		return String.format(Emoji.ACCESS_DENIED
				+ " (**%s**) I can't execute this command due to the lack of permission."
				+ "%nPlease, check my permissions and channel-specific ones to verify that %s is checked.",
				username, String.format("**%s**", StringUtils.capitalizeEnum(permission)));
	}

	public static String mustBeNsfw(String prefix) {
		return String.format(Emoji.GREY_EXCLAMATION
				+ " This must be a NSFW-channel. If you're an admin, you can use `%ssetting %s enable`",
				prefix, SettingEnum.NSFW);
	}
}

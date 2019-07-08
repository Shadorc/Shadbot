package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.TextFile;
import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.util.Permission;
import org.jsoup.Jsoup;
import reactor.util.annotation.NonNull;

import java.util.List;

public class TextUtils {

    public static final String NOT_ENOUGH_COINS =
            "You don't have enough coins. You can get some by playing **RPS**, **Hangman** or **Trivia**.";

    public static final String PLAYLIST_LIMIT_REACHED =
            String.format(Emoji.LOCK + " You've reached the maximum number of tracks (%d) in a playlist. "
                            + "You can remove this limit and gain other advantage by contributing to Shadbot. More info here: <%s>",
                    Config.DEFAULT_PLAYLIST_SIZE, Config.PATREON_URL);

    public static final TextFile SPAMS = new TextFile("texts/spam.txt");

    public static final List<String> TIP_MESSAGES = List.of(String.format("Check %slottery", Config.DEFAULT_PREFIX),
            String.format("Add a music first using %splayfirst", Config.DEFAULT_PREFIX),
            String.format("Help me keep Shadbot alive! %s", Config.PATREON_URL),
            String.format("Support server: %s", Config.SUPPORT_SERVER_URL),
            "The Baguette is a Lie");

    /**
     * @param err - the exception containing the error message to clean
     * @return The error message trimmed, without HTML tags nor YouTube links
     */
    public static String cleanLavaplayerErr(@NonNull FriendlyException err) {
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
                prefix, Setting.NSFW);
    }
}

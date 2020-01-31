package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.TextFile;
import com.shadorc.shadbot.object.Emoji;
import org.jsoup.Jsoup;
import reactor.util.annotation.NonNull;

public final class TextUtils {

    public static final String NOT_ENOUGH_COINS =
            "You don't have enough coins. You can get some by playing **RPS**, **Hangman** or **Trivia**.";

    public static final String PLAYLIST_LIMIT_REACHED =
            String.format(Emoji.LOCK + " You've reached the maximum number of tracks (%d) in a playlist. "
                    + "You can **remove this limit and gain other advantage** by contributing to Shadbot. "
                    + "More info here: <%s>", Config.PLAYLIST_SIZE, Config.PATREON_URL);

    public static final TextFile SPAMS = new TextFile("texts/spam.txt");
    public static final TextFile TIPS = new TextFile("texts/tips.txt");

    /**
     * @param err The exception containing the error message to clean.
     * @return The error message trimmed, without HTML tags nor YouTube links.
     */
    public static String cleanLavaplayerErr(@NonNull FriendlyException err) {
        return Jsoup.parse(StringUtils.remove(err.getMessage(), "Watch on YouTube")).text().trim();
    }

    public static String mustBeNsfw(String prefix) {
        return String.format(Emoji.GREY_EXCLAMATION
                        + " This must be a NSFW-channel. If you're an admin, you can use `%ssetting %s enable`",
                prefix, Setting.NSFW);
    }
}

package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.TextFile;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import discord4j.common.util.Snowflake;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.gateway.StatusUpdate;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.function.Consumer;

public class ShadbotUtil {

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
        if (err.getMessage() == null) {
            return "Error not specified.";
        }
        return Jsoup.parse(StringUtil.remove(err.getMessage(), "Watch on YouTube")).text().trim();
    }

    public static String mustBeNsfw() {
        return String.format(Emoji.GREY_EXCLAMATION
                + " This must be a NSFW-channel. If you're an admin, you can use `/setting %s enable`", Setting.NSFW);
    }

    /**
     * @return A random status update showing "Playing {prefix}help | {tip}"
     */
    public static StatusUpdate getRandomStatus() {
        final String presence = String.format("/help | %s", TIPS.getRandomLineFormatted());
        return Presence.online(Activity.playing(presence));
    }

    /**
     * @param guildId The {@link Snowflake} ID of the {@link discord4j.core.object.entity.Guild} in which the
     *                {@link discord4j.core.object.entity.User} made the bet.
     * @param userId  The {@link Snowflake} ID of the {@link discord4j.core.object.entity.User} who made the bet.
     * @param betStr  The string representing the bet.
     * @return A long representing {@code betStr}.
     * @throws CommandException thrown if {@code betStr} cannot be casted to a long or if the user does not have
     *                          enough coins.
     */
    public static Mono<Long> requireValidBet(Snowflake guildId, Snowflake userId, String betStr) {
        final Long bet = NumberUtil.toPositiveLongOrNull(betStr);
        if (bet == null) {
            throw new CommandException(String.format("`%s` is not a valid amount of coins.", betStr));
        }
        return ShadbotUtil.requireValidBet(guildId, userId, bet);
    }

    /**
     * @param guildId The {@link Snowflake} ID of the {@link discord4j.core.object.entity.Guild} in which the
     *                {@link discord4j.core.object.entity.User} made the bet.
     * @param userId  The {@link Snowflake} ID of the {@link discord4j.core.object.entity.User} who made the bet.
     * @param bet     The bet.
     * @return The bet.
     * @throws CommandException thrown if the user does not have enough coins.
     */
    public static Mono<Long> requireValidBet(Snowflake guildId, Snowflake userId, long bet) {
        return DatabaseManager.getGuilds()
                .getDBMember(guildId, userId)
                .map(DBMember::getCoins)
                .map(coins -> {
                    if (coins < bet) {
                        throw new CommandException(NOT_ENOUGH_COINS);
                    }
                    return bet;
                });
    }

    /**
     * @return A default {@link EmbedCreateSpec} with the default color set.
     */
    public static Consumer<EmbedCreateSpec> getDefaultEmbed(Consumer<EmbedCreateSpec> embed) {
        return embed.andThen(spec -> spec.setColor(Config.BOT_COLOR));
    }

}

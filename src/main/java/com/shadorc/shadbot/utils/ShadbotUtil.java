package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.TextFile;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.DBMember;
import discord4j.common.util.Snowflake;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.function.Consumer;

public class ShadbotUtil {

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

    /**
     * @return A random client presence showing "Playing /help | {tip}"
     */
    public static ClientPresence getRandomStatus() {
        final String presence = String.format("/help | %s", TIPS.getRandomLineFormatted());
        return ClientPresence.online(ClientActivity.playing(presence));
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
            throw new CommandException("`%s` is not a valid amount of coins.".formatted(betStr));
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
                        throw new CommandException(
                                "You don't have enough coins. You can get some by playing **RPS**, **Hangman** or **Trivia**.");
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

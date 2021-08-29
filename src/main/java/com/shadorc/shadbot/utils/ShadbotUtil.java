package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.DBMember;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Locale;
import java.util.function.Consumer;

public class ShadbotUtil {

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
     * @param guildId The ID of the Guild in which the User made the bet.
     * @param userId  The ID of the User who bet.
     * @param bet     The bet.
     * @return The bet.
     * @throws CommandException thrown if the user does not have enough coins.
     */
    public static Mono<Long> requireValidBet(Locale locale, Snowflake guildId, Snowflake userId, long bet) {
        return DatabaseManager.getGuilds()
                .getDBMember(guildId, userId)
                .map(DBMember::getCoins)
                .map(coins -> {
                    if (coins < bet) {
                        throw new CommandException(I18nManager.localize(locale, "not.enough.coins"));
                    }
                    return bet;
                });
    }

    /**
     * @return A default {@link LegacyEmbedCreateSpec} with the default color set.
     */
    public static Consumer<LegacyEmbedCreateSpec> getDefaultEmbed(Consumer<LegacyEmbedCreateSpec> embed) {
        return embed.andThen(spec -> spec.setColor(Config.BOT_COLOR));
    }

}

package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.html.thisday.ThisDay;
import com.shadorc.shadbot.core.cache.SingleValueCache;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

public class ThisDayCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.onthisday.com/";

    private final SingleValueCache<ThisDay> getThisDay;

    public ThisDayCmd() {
        super(CommandCategory.FUN, "this_day", "Significant events of the day");
        this.getThisDay = SingleValueCache.Builder
                .create(ThisDayCmd.getThisDay())
                .withTtlForValue(__ -> ThisDayCmd.getNextUpdate())
                .build();

    }

    private static Mono<ThisDay> getThisDay() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(ThisDay::new);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.reply(Emoji.HOURGLASS, context.localize("thisday.loading"))
                .then(this.getThisDay)
                .flatMap(thisDay -> context.editReply(ThisDayCmd.formatEmbed(context, thisDay)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, ThisDay thisDay) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("thisday.title").formatted(thisDay.getDate()),
                        HOME_URL, context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/FdfyJDD.png")
                        .setDescription(StringUtil.abbreviate(thisDay.getEvents(), Embed.MAX_DESCRIPTION_LENGTH)));
    }

    private static Duration getNextUpdate() {
        ZonedDateTime nextDate = ZonedDateTime.now()
                .withHour(0)
                .withMinute(10) // Waits for the website to update, just in case
                .withSecond(0);
        if (nextDate.isBefore(ZonedDateTime.now())) {
            nextDate = nextDate.plusDays(1);
        }

        return TimeUtil.elapsed(nextDate.toInstant());
    }

}

package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.html.thisday.ThisDay;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

class ThisDayCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.onthisday.com/";

    public ThisDayCmd() {
        super(CommandCategory.FUN, "this_day", "Significant events of the day");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.reply(Emoji.HOURGLASS, context.localize("thisday.loading"))
                .then(ThisDayCmd.getThisDay())
                .flatMap(thisDay -> context.editReply(ThisDayCmd.formatEmbed(context, thisDay)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, ThisDay thisDay) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("thisday.title").formatted(thisDay.getDate()),
                        HOME_URL, context.getAuthorAvatar())
                        .setThumbnail("https://i.imgur.com/FdfyJDD.png")
                        .setDescription(StringUtil.abbreviate(thisDay.getEvents(), Embed.MAX_DESCRIPTION_LENGTH)));
    }

    // TODO: Cache the value and clean the cache once a day
    private static Mono<ThisDay> getThisDay() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(ThisDay::new);
    }

}

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
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class ThisDayCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.onthisday.com/";

    public ThisDayCmd() {
        super(CommandCategory.FUN, "this_day", "Show significant events of the day");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading events...", context.getAuthorName())
                .zipWith(ThisDayCmd.getThisDay())
                .flatMap(TupleUtils.function((messageId, thisDay) ->
                        context.editFollowupMessage(messageId, ShadbotUtil.getDefaultEmbed(
                                embed -> embed.setAuthor(String.format("On This Day: %s", thisDay.getDate()),
                                        HOME_URL, context.getAuthorAvatarUrl())
                                        .setThumbnail("https://i.imgur.com/FdfyJDD.png")
                                        .setDescription(StringUtil.abbreviate(thisDay.getEvents(), Embed.MAX_DESCRIPTION_LENGTH))))));
    }

    private static Mono<ThisDay> getThisDay() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(ThisDay::new);
    }

}

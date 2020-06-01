package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.html.thisday.ThisDay;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class ThisDayCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.onthisday.com/";

    public ThisDayCmd() {
        super(CommandCategory.FUN, List.of("this_day"), "td");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading events...", context.getUsername()))
                .send()
                .then(this.getThisDay())
                .map(thisDay -> updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor(String.format("On This Day: %s",
                                thisDay.getDate()), HOME_URL, context.getAvatarUrl())
                                .setThumbnail("https://i.imgur.com/FdfyJDD.png")
                                .setDescription(StringUtils.abbreviate(thisDay.getEvents(), Embed.MAX_DESCRIPTION_LENGTH)))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<ThisDay> getThisDay() {
        return NetUtils.get(HOME_URL)
                .map(Jsoup::parse)
                .map(ThisDay::new);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show significant events of the day.")
                .setSource(HOME_URL)
                .build();
    }
}

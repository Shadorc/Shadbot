package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.xkcd.XkcdResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class XkcdCmd extends BaseCmd {

    private enum Sort {
        LATEST, RANDOM
    }

    private static final String HOME_URL = "https://xkcd.com";
    private static final String LAST_URL = "%s/info.0.json".formatted(HOME_URL);

    // Cache for the latest Xkcd ID
    private final AtomicInteger latestId;

    public XkcdCmd() {
        super(CommandCategory.IMAGE, "xkcd", "Show random comic from XKCD");
        this.latestId = new AtomicInteger();

        final List<ApplicationCommandOptionChoiceData> choices = List.of(
                ApplicationCommandOptionChoiceData.builder().name("latest").value("latest").build(),
                ApplicationCommandOptionChoiceData.builder().name("random").value("random").build());
        this.addOption("sort", "Sorting option", true, ApplicationCommandOptionType.STRING, choices);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Sort sort = EnumUtil.parseEnum(Sort.class, context.getOption("sort").orElseThrow().asString());
        final Mono<XkcdResponse> getResponse = sort == Sort.LATEST ? XkcdCmd.getLatestXkcd() : this.getRandomXkcd();
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading XKCD comic...", context.getAuthorName())
                .flatMap(messageId -> getResponse.flatMap(xkcd -> context.editReply(messageId,
                        XkcdCmd.formatEmbed(context.getAuthorAvatar(), xkcd))));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final XkcdResponse xkcd) {
        return ShadbotUtil.getDefaultEmbed(embed ->
                embed.setAuthor("XKCD: %s".formatted(xkcd.getTitle()), "%s/%d".formatted(HOME_URL, xkcd.getNum()), avatarUrl)
                        .setImage(xkcd.getImg()));
    }

    private Mono<XkcdResponse> getRandomXkcd() {
        return Mono.fromCallable(this.latestId::get)
                .filter(latestId -> latestId != 0)
                .switchIfEmpty(XkcdCmd.getLatestXkcd()
                        .map(XkcdResponse::getNum)
                        .doOnNext(this.latestId::set))
                .map(ThreadLocalRandom.current()::nextInt)
                .flatMap(id -> RequestHelper.fromUrl("%s/%d/info.0.json".formatted(HOME_URL, id))
                        .to(XkcdResponse.class));
    }

    private static Mono<XkcdResponse> getLatestXkcd() {
        return RequestHelper.fromUrl(LAST_URL)
                .to(XkcdResponse.class);
    }

}

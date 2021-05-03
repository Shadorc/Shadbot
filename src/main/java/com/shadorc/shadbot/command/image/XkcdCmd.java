package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.xkcd.XkcdResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

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

        this.addOption("sort", "Sorting option", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Sort.class));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Sort sort = context.getOptionAsEnum(Sort.class, "sort").orElseThrow();
        final Mono<XkcdResponse> getResponse = sort == Sort.LATEST ? XkcdCmd.getLatestXkcd() : this.getRandomXkcd();
        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("xkcd.loading"))
                .then(getResponse)
                .flatMap(xkcd -> context.editFollowupMessage(XkcdCmd.formatEmbed(context.getAuthorAvatar(), xkcd)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(String avatarUrl, XkcdResponse xkcd) {
        return ShadbotUtil.getDefaultEmbed(embed ->
                embed.setAuthor("XKCD: %s".formatted(xkcd.title()), "%s/%d".formatted(HOME_URL, xkcd.num()), avatarUrl)
                        .setImage(xkcd.img()));
    }

    private Mono<XkcdResponse> getRandomXkcd() {
        return Mono.fromCallable(this.latestId::get)
                .filter(latestId -> latestId != 0)
                .switchIfEmpty(XkcdCmd.getLatestXkcd()
                        .map(XkcdResponse::num)
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

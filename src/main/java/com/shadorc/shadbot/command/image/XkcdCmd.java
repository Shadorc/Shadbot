package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.xkcd.XkcdResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class XkcdCmd extends BaseCmd {

    private static final String HOME_URL = "https://xkcd.com";
    private static final String LAST_URL = HOME_URL + "/info.0.json";

    public XkcdCmd() {
        super(CommandCategory.IMAGE, List.of("xkcd"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading XKCD comic...", context.getUsername()))
                .send()
                .then(XkcdCmd.getRandomXkcd())
                .map(xkcd -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor(String.format("XKCD: %s", xkcd.getTitle()),
                                String.format("%s/%d", HOME_URL, xkcd.getNum()), context.getAvatarUrl())
                                .setImage(xkcd.getImg()))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<XkcdResponse> getRandomXkcd() {
        return NetUtils.get(LAST_URL, XkcdResponse.class)
                .map(XkcdResponse::getNum)
                .map(ThreadLocalRandom.current()::nextInt)
                .flatMap(rand -> NetUtils.get(String.format("%s/%d/info.0.json", HOME_URL, rand), XkcdResponse.class));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show a random XKCD comic.")
                .setSource(HOME_URL)
                .build();
    }
}

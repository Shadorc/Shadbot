package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.LoadingMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class SuicideGirlsCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.suicidegirls.com/photos/sg/recent/all/";

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, List.of("suicide_girls", "suicide-girls", "suicidegirls"), "sg");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return Mono.just(loadingMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix())));
                    }

                    return NetUtils.get(HOME_URL)
                            .map(Jsoup::parse)
                            .map(doc -> doc.getElementsByTag("article"))
                            .map(Utils::randValue)
                            .map(girl -> {
                                final String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
                                final String imageUrl = girl.select("noscript").attr("data-retina");
                                final String url = girl.getElementsByClass("facebook-share").attr("href");

                                return loadingMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                                        .andThen(embed -> embed.setAuthor("SuicideGirls", url, context.getAvatarUrl())
                                                .setDescription(String.format("Name: **%s**", StringUtils.capitalize(name)))
                                                .setImage(imageUrl)));
                            });
                })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show a random Suicide Girl image.")
                .setSource("https://www.suicidegirls.com/")
                .build();
    }

}

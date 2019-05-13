package me.shadorc.shadbot.command.image;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class SuicideGirlsCmd extends BaseCmd {

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, List.of("suicide_girls", "suicide-girls", "suicidegirls"), "sg");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

        return context.isChannelNsfw()
                .flatMap(isNsfw -> Mono.fromCallable(() -> {
                    if (!isNsfw) {
                        return loadingMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix()));
                    }

                    final Document doc = NetUtils.getDocument("https://www.suicidegirls.com/photos/sg/recent/all/");

                    final Element girl = Utils.randValue(doc.getElementsByTag("article"));
                    final String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
                    final String imageUrl = girl.select("noscript").attr("data-retina");
                    final String url = girl.getElementsByClass("facebook-share").attr("href");

                    return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("SuicideGirls", url, context.getAvatarUrl())
                                    .setDescription(String.format("Name: **%s**", StringUtils.capitalize(name)))
                                    .setImage(imageUrl)));
                }))
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

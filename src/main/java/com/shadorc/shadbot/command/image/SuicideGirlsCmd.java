package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.html.suicidegirl.SuicideGirl;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.function.Consumer;

public class SuicideGirlsCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.suicidegirls.com/photos/sg/recent/all/";

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, "suicidegirls", "Show random image from SuicideGirls");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(ShadbotUtil.mustBeNsfw());
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading SuicideGirls image...", context.getAuthorName())
                            .zipWith(SuicideGirlsCmd.getRandomSuicideGirl())
                            .flatMap(TupleUtils.function((messageId, post) -> context.editReply(messageId,
                                    SuicideGirlsCmd.formatEmbed(context.getAuthorAvatar(), post))));
                });
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final SuicideGirl post) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("SuicideGirls", post.getUrl(), avatarUrl)
                        .setDescription("Name: **%s**".formatted(post.getName()))
                        .setImage(post.getImageUrl()));
    }

    private static Mono<SuicideGirl> getRandomSuicideGirl() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(SuicideGirl::new);
    }

}

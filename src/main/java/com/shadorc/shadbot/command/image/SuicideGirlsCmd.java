package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.html.suicidegirl.SuicideGirl;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class SuicideGirlsCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.suicidegirls.com/photos/sg/recent/all/";

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, "suicide_girls", "Show a random Suicide Girl image");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(ShadbotUtil.mustBeNsfw());
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading Suicide Girl picture...", context.getAuthorName())
                            .zipWith(SuicideGirlsCmd.getRandomSuicideGirl())
                            .flatMap(TupleUtils.function((messageId, post) ->
                                    context.editFollowupMessage(messageId, ShadbotUtil.getDefaultEmbed(
                                            embed -> embed.setAuthor("SuicideGirls", post.getUrl(), context.getAuthorAvatarUrl())
                                                    .setDescription(String.format("Name: **%s**", post.getName()))
                                                    .setImage(post.getImageUrl())))));
                });
    }

    private static Mono<SuicideGirl> getRandomSuicideGirl() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(SuicideGirl::new);
    }

}

package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.xkcd.XkcdResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

public class XkcdCmd extends BaseCmd {

    private static final String HOME_URL = "https://xkcd.com";
    private static final String LAST_URL = String.format("%s/info.0.json", HOME_URL);

    public XkcdCmd() {
        super(CommandCategory.IMAGE, "xkcd", "Show a random XKCD comic");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading XKCD comic...", context.getAuthorName())
                .flatMap(messageId -> XkcdCmd.getRandomXkcd()
                        .flatMap(xkcd -> context.editFollowupMessage(messageId,
                                ShadbotUtils.getDefaultEmbed(spec -> spec.setAuthor(
                                        String.format("XKCD: %s", xkcd.getTitle()),
                                        String.format("%s/%d", HOME_URL, xkcd.getNum()),
                                        context.getAuthorAvatarUrl())
                                        .setImage(xkcd.getImg())))));
    }

    private static Mono<XkcdResponse> getRandomXkcd() {
        return RequestHelper.fromUrl(LAST_URL)
                .to(XkcdResponse.class)
                .map(XkcdResponse::getNum)
                .map(ThreadLocalRandom.current()::nextInt)
                .flatMap(rand -> RequestHelper.fromUrl(String.format("%s/%d/info.0.json", HOME_URL, rand))
                        .to(XkcdResponse.class));
    }

}

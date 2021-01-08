/*
package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.html.suicidegirl.SuicideGirl;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class SuicideGirlsCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.suicidegirls.com/photos/sg/recent/all/";

    public SuicideGirlsCmd() {
        super(CommandCategory.IMAGE, List.of("suicide_girls"), "sg");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(
                String.format(Emoji.HOURGLASS + " (**%s**) Loading Suicide Girl picture...", context.getUsername()))
                .send()
                .then(context.isChannelNsfw())
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return Mono.just(updatableMsg.setContent(ShadbotUtils.mustBeNsfw(context.getPrefix())));
                    }

                    return SuicideGirlsCmd.getRandomSuicideGirl()
                            .map(girl -> updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                                    .andThen(embed -> embed.setAuthor("SuicideGirls", girl.getUrl(), context.getAvatarUrl())
                                            .setDescription(String.format("Name: **%s**", girl.getName()))
                                            .setImage(girl.getImageUrl()))));
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<SuicideGirl> getRandomSuicideGirl() {
        return RequestHelper.request(HOME_URL)
                .map(Jsoup::parse)
                .map(SuicideGirl::new);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show a random Suicide Girl image.")
                .setSource("https://www.suicidegirls.com/")
                .build();
    }

}
*/
